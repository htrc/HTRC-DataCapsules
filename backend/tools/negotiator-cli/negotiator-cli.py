"""
Usage: negotiator-cli [OPTIONS] GUEST_UNIX_SOCKET
Communicate from a KVM/QEMU host system with running guest systems using a
guest agent daemon running inside the guests.
Supported options:
  -c, --list-commands
    List the commands that the guest exposes to its host.
  -e, --execute=COMMAND
    Execute the given command inside GUEST_UNIX_SOCKET. The standard output stream of
    the command inside the guest is intercepted and copied to the standard
    output stream on the host. If the command exits with a nonzero status code
    the negotiator-host program will also exit with a nonzero status code.
  -t, --timeout=SECONDS
    Set the number of seconds before a remote call without a response times
    out. A value of zero disables the timeout (in this case the command can
    hang indefinitely). The default is 10 seconds.
  -h, --help
    Show this message and exit.
"""

from humanfriendly import Timer
from negotiator_common.config import DEFAULT_TIMEOUT
from negotiator_common import NegotiatorInterface
from negotiator_common.utils import TimeOut

import coloredlogs
import functools
import getopt
import logging
import os
import shlex
import socket
import sys


# Initialize a logger for this module.
logger = logging.getLogger(__name__)

class GuestChannel(NegotiatorInterface):

    """
    The host side of the channel connecting KVM/QEMU hosts and guests. 
    This is a modificaiton of negotiator_host.GuestChannel
    """

    def __init__(self, unix_socket):
        if not unix_socket:
            raise GuestChannelInitializationError("No UNIX socket pathname provided!")

        # Connect to the UNIX socket.
        logger.debug("Opening UNIX socket: %s", unix_socket)
        self.socket = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        try:
            logger.debug("Connecting to UNIX socket: %s", unix_socket)
            self.socket.connect(unix_socket)
        except Exception:
            raise GuestChannelInitializationError("Guest refused connection attempt!")
        logger.debug("Successfully connected to UNIX socket!")
        # Initialize the super class, passing it a file like object connected
        # to the character device in read/write mode.
        super(GuestChannel, self).__init__(handle=self.socket.makefile(),
                                           label="UNIX socket %s" % unix_socket)

    def prepare_environment(self):
        """
        Prepare environment variables for command execution on KVM/QEMU hosts.
        The following environment variables are currently exposed to commands:
        ``$NEGOTIATOR_GUEST``
          The name of the KVM/QEMU guest that invoked the command.
        """
        os.environ['NEGOTIATOR_GUEST'] = self.guest_name


class GuestChannelInitializationError(Exception):

    """Exception raised by :py:class:`GuestChannel` when socket initialization fails."""


class Context(object):

    """Enables :py:func:`main()` to inject a custom timeout into partially applied actions."""

    def __init__(self):
        """Initialize a context for executing commands on the host."""
        self.timeout = DEFAULT_TIMEOUT

    def print_commands(self, guest_unix_socket):
        """Print the commands supported by the guest."""
        with TimeOut(self.timeout):
            channel = GuestChannel(unix_socket=guest_unix_socket)
            print('\n'.join(sorted(channel.call_remote_method('list_commands'))))

    def execute_command(self, guest_unix_socket, command_line):
        """Execute a command inside the named guest."""
        with TimeOut(self.timeout):
            timer = Timer()
            channel = GuestChannel(unix_socket=guest_unix_socket)
            output = channel.call_remote_method('execute', *shlex.split(command_line), capture=True)
            logger.debug("Took %s to execute remote command.", timer)
            print(output.rstrip())

def main():
    """Command line interface for the ``negotiator-cli`` program."""
     # Initialize logging to the terminal and system log.
    coloredlogs.install(syslog=True)
    # Parse the command line arguments.
    actions = []
    context = Context()
    try:
        options, arguments = getopt.getopt(sys.argv[1:], 'ce:t:h', [
            'list-commands', 'execute=', 'timeout=', 'help'
        ])
        for option, value in options:
            if option in ('-c', '--list-commands'):
                assert len(arguments) == 1, \
                    "Please provide the unix socket of a guest as the 1st and only positional argument!"
                actions.append(functools.partial(context.print_commands, arguments[0]))
            elif option in ('-e', '--execute'):
                assert len(arguments) == 1, \
                    "Please provide the unix socket of a guest as the 1st and only positional argument!"
                actions.append(functools.partial(context.execute_command, arguments[0], value))
            elif option in ('-t', '--timeout'):
                context.timeout = int(value)
            elif option in ('-h', '--help'):
                usage()
                sys.exit(0)
        if not actions:
            usage()
            sys.exit(0)
    except Exception:
        logger.exception("Failed to parse command line arguments!")
        sys.exit(1)
    # Execute the requested action(s).
    try:
        for action in actions:
            action()
    except Exception:
        logger.exception("Caught a fatal exception! Terminating ..")
        sys.exit(1)


def usage():
    """Print a user friendly usage message to the terminal."""
    print(__doc__.strip())

if __name__ == "__main__":
    main()