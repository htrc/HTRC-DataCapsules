#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/wait.h>

/* BEGIN Code taken from linux/Documentation/networking/tuntap.txt */
#include <sys/ioctl.h>

#include <linux/if_tun.h>
#include <linux/if.h>

#define INIT_SCRIPT	"/etc/qemu-ifup"
#define FDSTRING	"%FD%"
#define ARGLEN		256

int tun_alloc(char *dev)
{
	struct ifreq ifr;
	int fd, err;

	if( (fd = open("/dev/net/tun", O_RDWR)) < 0 )
		return -1;

	memset(&ifr, 0, sizeof(ifr));

	/* Flags: IFF_TUN   - TUN device (no Ethernet headers) 
	*        IFF_TAP   - TAP device  
	*
	*        IFF_NO_PI - Do not provide packet information  
	*/ 
	ifr.ifr_flags = IFF_TAP | IFF_NO_PI;
	if( *dev )
		strncpy(ifr.ifr_name, dev, IFNAMSIZ);

	if( (err = ioctl(fd, TUNSETIFF, (void *) &ifr)) < 0 )
	{
		close(fd);
		return err;
	}

	strcpy(dev, ifr.ifr_name);
	return fd;
}
/* END Code taken from linux/Documentation/networking/tuntap.txt */

/* BEGIN Code taken from qemu */
static int launch_script(const char *setup_script, const char *ifname, int fd)
{
    int pid, status;
    char *args[3];
    char **parg;

    /* try to launch network script */
    pid = fork();
    if (pid == 0) {
        int open_max = sysconf(_SC_OPEN_MAX), i;

        for (i = 0; i < open_max; i++) {
            if (i != STDIN_FILENO &&
                i != STDOUT_FILENO &&
                i != STDERR_FILENO &&
                i != fd) {
                close(i);
            }
        }
        parg = args;
        *parg++ = (char *)setup_script;
        *parg++ = (char *)ifname;
        *parg = NULL;
        execv(setup_script, args);
        _exit(1);
    } else if (pid > 0) {
        while (waitpid(pid, &status, 0) != pid) {
            /* loop */
        }

        if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
            return 0;
        }
    }
    fprintf(stderr, "%s: could not launch network script\n", setup_script);
    return -1;
}
/* END Code taken from qemu */

int main(int argc, char **argv)
{
	uid_t caller_uid;

	char tapdev[11] = "tap%d";
	int tapfd;

	char **newargv;
	char newargstr[ARGLEN];
	char *fdpos;
	int i;

	if(argc < 2)
		return 1;

	caller_uid = getuid();

	tapfd = tun_alloc((char*) &tapdev);

	if(tapfd < 0)
		return 1;

	if(launch_script(INIT_SCRIPT,(char *) &tapdev, tapfd))
		return 1;

	if(seteuid(caller_uid) != 0)
		return 1;

	newargv = argv + 1;

	// Insert fd into command line
	for(i = 0; i < argc-1; i++)
		if(newargv[i] != NULL && (fdpos = strstr(newargv[i], FDSTRING)) != NULL)
		{
			*(fdpos) = '\0';

			snprintf(newargstr, ARGLEN, "%s%d%s", newargv[i], tapfd, fdpos+4);

			newargv[i]=newargstr;
			break;
		}

	return execvp(newargv[0], newargv);
}
