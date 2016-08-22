#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

/* BEGIN Code taken from
 * https://www.kernel.org/doc/Documentation/networking/tuntap.txt */
#include <sys/ioctl.h>

#include <linux/if.h>
#include <linux/if_tun.h>

#define INIT_SCRIPT "/etc/qemu-ifup"
#define FDSTRING "%FD%"
#define ARGLEN 256

int tun_alloc(char *dev) {
  struct ifreq ifr;
  int fd, err;

  if ((fd = open("/dev/net/tun", O_RDWR)) < 0) return -1;

  memset(&ifr, 0, sizeof(ifr));

  /* Flags: IFF_TUN   - TUN device (no Ethernet headers)
  *        IFF_TAP   - TAP device
  *
  *        IFF_NO_PI - Do not provide packet information
  */
  ifr.ifr_flags = IFF_TAP | IFF_NO_PI;
  if (*dev) strncpy(ifr.ifr_name, dev, IFNAMSIZ);

  if ((err = ioctl(fd, TUNSETIFF, (void *)&ifr)) < 0) {
    fprintf(stderr, "tapinit: unable to allocate TAP device\n");
    close(fd);
    return err;
  }

  strcpy(dev, ifr.ifr_name);
  return fd;
}
/* END Code taken from
 * https://www.kernel.org/doc/Documentation/networking/tuntap.txt */

/* BEGIN Code taken from qemu */
static int launch_script(const char *setup_script, const char *ifname,
                         const char *script_dir, const char *guest_ip, int fd,
                         uid_t parent_euid) {
  int pid, status;
  char *args[5];
  char **parg;

  /* try to launch network script */
  pid = fork();
  if (pid == 0) {
    int open_max = sysconf(_SC_OPEN_MAX), i;

    for (i = 0; i < open_max; i++) {
      if (i != STDIN_FILENO && i != STDOUT_FILENO && i != STDERR_FILENO &&
          i != fd) {
        close(i);
      }
    }
    parg = args;
    *parg++ = (char *)setup_script;
    *parg++ = (char *)ifname;
    *parg++ = (char *)script_dir;
    *parg++ = (char *)guest_ip;
    *parg = NULL;

    /*
     * SUID is not allowed for shell scripts in modern Unix/Linux operating systems due to security risks. 
     * If real user id (RUID) and effective user id (EUID) is different when executing a shell script OS 
     * will automatically set EUID of running shell script to RUID. So we need to call setuid before executing
     * shell scripts via exec family of calls. More information can be found in following links:
     *  - http://www.faqs.org/faqs/unix-faq/faq/part4/section-7.html
     *  - http://unix.stackexchange.com/questions/364/allow-setuid-on-shell-scripts
     *  - http://stackoverflow.com/questions/18698976/suid-not-working-with-shell-script 
     */
    setuid(parent_euid);
    execve(setup_script, args, NULL);
    _exit(1);
  } else if (pid > 0) {
    while (waitpid(pid, &status, 0) != pid) {
      /* loop */
    }

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
      return 0;
    }
    if (WIFEXITED(status) != 0) {
      fprintf(stderr, "%s: could not launch network script\n", setup_script);
    }
    if (WEXITSTATUS(status) != 0) {
      fprintf(stderr, "%s: network script returned error code %d\n",
              setup_script, WEXITSTATUS(status));
    }
  }
  return -1;
}
/* END Code taken from qemu */

int main(int argc, char **argv) {
  uid_t caller_uid;
  uid_t effective_uid;

  char tapdev[11] = "tap%d";
  int tapfd;

  char **newargv;
  char newargstr[ARGLEN];
  char *fdpos;
  int i;

  if (argc < 4) return 1;

  caller_uid = getuid();
  effective_uid = geteuid();

  if (effective_uid != 0) {
    fprintf(stderr, "tapinit: must be run as setuid root!\n");
    return 1;
  }

  tapfd = tun_alloc((char *)&tapdev);

  if (tapfd < 0) {
    fprintf(stderr, "tapinit: could not create tap device!\n");
    return 1;
  }

  if (launch_script(INIT_SCRIPT, (char *)&tapdev, argv[1], argv[2], tapfd,
                    effective_uid))
    return 1;

  if (seteuid(caller_uid) != 0) {
    fprintf(stderr,
            "tapinit: could not set effective uid back to caller id!\n");
    return 1;
  }

  newargv = argv + 3;

  // Insert fd into command line
  for (i = 0; i < argc - 3; i++)
    if (newargv[i] != NULL && (fdpos = strstr(newargv[i], FDSTRING)) != NULL) {
      *(fdpos) = '\0';

      snprintf(newargstr, ARGLEN, "%s%d%s", newargv[i], tapfd, fdpos + 4);

      newargv[i] = newargstr;
      break;
    }

  return execvp(newargv[0], newargv);
}
