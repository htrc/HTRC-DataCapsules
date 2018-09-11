import os
import sys
import json
import urllib
import urllib2
import argparse
import string
import httplib
import time

# DC
DC_API = 'localhost'
PORT = '80'

def query_yes_no(question, default="yes"):
    """Ask a yes/no question via raw_input() and return their answer.
    "question" is a string that is presented to the user.
    "default" is the presumed answer if the user just hits <Enter>.
        It must be "yes" (the default), "no" or None (meaning
        an answer is required of the user).
    The "answer" return value is True for "yes" or False for "no".
    """
    valid = {"yes": True, "y": True, "ye": True,
             "no": False, "n": False}
    if default is None:
        prompt = " [y/n] "
    elif default == "yes":
        prompt = " [Y/n] "
    elif default == "no":
        prompt = " [y/N] "
    else:
        raise ValueError("invalid default answer: '%s'" % default)

    while True:
        sys.stdout.write(question + prompt)
        choice = raw_input().lower()
        if default is not None and choice == '':
            return valid[default]
        elif choice in valid:
            return valid[choice]
        else:
            sys.stdout.write("Please respond with 'yes' or 'no' "
                             "(or 'y' or 'n').\n")


def delete_vm(vmid, username, useremail):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

    params = urllib.urlencode({'vmid': vmid})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/deletevm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data


def stop_vm(vmid, username, useremail):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

    params = urllib.urlencode({'vmid': vmid})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/stopvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data


def start_vm(vmid, username, useremail):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

    params = urllib.urlencode({'vmid': vmid})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/launchvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data


def switch_vm(vmid, username, useremail, mode):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

    params = urllib.urlencode({'vmid': vmid, 'mode': mode})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/switchvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data


def create_vm(username, useremail, imagename, loginusername, loginpassword, memory, vcpu):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

    params = urllib.urlencode(
        {'imagename': imagename, 'loginusername': loginusername, 'loginpassword': loginpassword, 'memory': memory,
         'vcpu': vcpu})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/createvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data


def show_release():
    headers = {'Content-Type': 'application/json'}

    # GET the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/showreleased')
    response = conn.getresponse()

    data = response.read()
    parsed = json.loads(data)
    print json.dumps(parsed, indent=4, sort_keys=True)


def show_unrelease():
    headers = {'Content-Type': 'application/json'}

    # GET the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/showunreleased')
    response = conn.getresponse()

    data = response.read()
    parsed = json.loads(data)
    print json.dumps(parsed, indent=4, sort_keys=True)


def retrieve_file(result_id, out_file):
    # GET the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/retrieveresultfile?randomid=' + result_id)
    response = conn.getresponse()

    if (response.status != 200):
        print response.read()
    else:
        data = response.read()
        f = open(out_file, 'w')
        f.write(data)
        f.close()
        print 'Result written to ' + out_file + ' file...'


def download_file(result_id, out_file):
    # GET the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/download?randomid=' + result_id)
    response = conn.getresponse()

    if (response.status != 200):
        print response.read()
    else:
        data = response.read()
        f = open(out_file, 'w')
        f.write(data)
        f.close()
        print 'Result downloaded to ' + out_file + ' file...'


def update_result(result_id, status):
    params = urllib.urlencode({'resultid': result_id, 'status': status})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/updateresult', params)
    response = conn.getresponse()

    print response.read()


def stop_running_vms():
    headers = {'Content-Type': 'application/json',
               'Accept': 'application/json'}

    # Get request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/listvms')
    response = conn.getresponse()

    if response.status == 200:
        vms = json.loads(response.read())['vmsInfo']

        for vm in vms:
            if vm["vmState"] == "RUNNING":
                print 'Stopping VM: {}'.format(vm["vmid"])
                stop_vm(vm["vmid"], vm["username"], vm["userEmail"])
                time.sleep(5)


def update_vmtype(vmid, username, status):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    params = urllib.urlencode({'vmId': vmid, 'type': 'RESEARCH-FULL', 'full_access': status})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/updatevm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data


def show_capsules(username):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/show', "", headers)
    response = conn.getresponse()

    data = response.read()
    parsed = json.loads(data)
    print json.dumps(parsed, indent=4, sort_keys=True)


def show_pending_fullaccess(username):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/show', "", headers)
    response = conn.getresponse()

    if response.status == 200:
        vms = json.loads(response.read())['status']

        for vm in vms:
            if vm["full_access"] is not None and vm["full_access"] is False:
                print 'VM ID : {} has pending request for full data access.'.format(vm["vmid"])


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest='sub_commands')
    delete = subparsers.add_parser('delete', description='Delete DC VM')
    delete.add_argument('vm')
    delete.add_argument('vmuser')
    delete.add_argument('vmuseremail')

    stop = subparsers.add_parser('stop', description='Stop DC VM')
    stop.add_argument('vm')
    stop.add_argument('vmuser')
    stop.add_argument('vmuseremail')

    start = subparsers.add_parser('start', description='Start DC VM')
    start.add_argument('vm')
    start.add_argument('vmuser')
    start.add_argument('vmuseremail')

    switch = subparsers.add_parser('switch', description='Switch DC VM')
    switch.add_argument('vm')
    switch.add_argument('vmuser')
    switch.add_argument('vmuseremail')
    switch.add_argument('mode')

    create = subparsers.add_parser('create', description='Create DC VM')
    create.add_argument('vmuser')
    create.add_argument('vmuseremail')
    create.add_argument('imagename')
    create.add_argument('vncusername')
    create.add_argument('vncpassword')
    create.add_argument('memory')
    create.add_argument('vcpu')

    showrelease = subparsers.add_parser('showrelease', description='Show released results')

    showunrelease = subparsers.add_parser('showunrelease', description='Show un-released results')

    retrievefile = subparsers.add_parser('retrievefile', description='Get result file')
    retrievefile.add_argument('rid')
    retrievefile.add_argument('filename')

    downloadfile = subparsers.add_parser('downloadfile', description='Download result file')
    downloadfile.add_argument('rid')
    downloadfile.add_argument('filename')

    releaseresult = subparsers.add_parser('releaseresult', description='Release the result')
    releaseresult.add_argument('rid')

    rejectresult = subparsers.add_parser('rejectresult', description='Reject the result')
    rejectresult.add_argument('rid')

    subparsers.add_parser("stoprunning", description="Stop all running capsules")

    approvefullaccess = subparsers.add_parser('approvefullaccess', description='Approve Full Access for Data')
    approvefullaccess.add_argument('vm')
    approvefullaccess.add_argument('vmuser')

    rejectfullaccess = subparsers.add_parser('rejectfullaccess', description='Reject Full Access for Data')
    rejectfullaccess.add_argument('vm')
    rejectfullaccess.add_argument('vmuser')

    showcapsules = subparsers.add_parser('showcapsules', description='Show all capsules for the given user')
    showcapsules.add_argument('vmuser')

    showpendingfullaccess = subparsers.add_parser('showpendingfullaccess', description='Show VM IDs which have pending requests for full data access.')
    showpendingfullaccess.add_argument('vmuser')

    parsed = parser.parse_args()

    if parsed.sub_commands == 'delete':
        confirmation = query_yes_no(
            'Are you sure you want to delete the VM ' + parsed.vm + '? This operation is not recoverable.')
        if confirmation:
            print 'Deleting  VM ' + parsed.vm + '....'
            delete_vm(parsed.vm, parsed.vmuser, parsed.vmuseremail)

    if parsed.sub_commands == 'stop':
        confirmation = query_yes_no('Are you sure you want to stop the VM ' + parsed.vm + '?')
        if confirmation:
            print 'Stopping  VM ' + parsed.vm + '....'
            stop_vm(parsed.vm, parsed.vmuser, parsed.vmuseremail)

    if parsed.sub_commands == 'start':
        confirmation = query_yes_no('Are you sure you want to start the VM ' + parsed.vm + '?')
        if confirmation:
            print 'Starting  VM ' + parsed.vm + '....'
            start_vm(parsed.vm, parsed.vmuser, parsed.vmuseremail)

    if parsed.sub_commands == 'switch':
        confirmation = query_yes_no(
            'Are you sure you want to switch the VM ' + parsed.vm + ' to ' + parsed.mode + ' mode?')
        if confirmation:
            print 'Switching  VM ' + parsed.vm + ' to ' + parsed.mode + '....'
            switch_vm(parsed.vm, parsed.vmuser, parsed.vmuseremail, parsed.mode)

    if parsed.sub_commands == 'create':
        confirmation = query_yes_no(
            'Are you sure you want to create a VM with ' + parsed.imagename + ',' + parsed.memory + ',' + parsed.vcpu + ' ?')
        if confirmation:
            print 'Creating  VM with image:' + parsed.imagename + ', VNC User name:' + parsed.vncusername + ', VNC Password:' + parsed.vncpassword + ', memory: ' + parsed.memory + ', vcpu: ' + parsed.vcpu + '...'
            create_vm(parsed.vmuser, parsed.vmuseremail, parsed.imagename, parsed.vncusername, parsed.vncpassword,
                      parsed.memory, parsed.vcpu)

    if parsed.sub_commands == 'showrelease':
        print 'Released Results:'
        show_release()

    if parsed.sub_commands == 'showunrelease':
        print 'Un-Released Results:'
        show_unrelease()

    if parsed.sub_commands == 'retrievefile':
        confirmation = query_yes_no('Are you sure you want to retrieve file with id ' + parsed.rid)
        if confirmation:
            print 'Retrieving file for result ' + parsed.rid + '....'
            retrieve_file(parsed.rid, parsed.filename)

    if parsed.sub_commands == 'downloadfile':

        confirmation = query_yes_no('Are you sure you want to download file with id ' + parsed.rid)
        if confirmation:
            print 'Download file for result ' + parsed.rid + '....'
            download_file(parsed.rid, parsed.filename)

    if parsed.sub_commands == 'releaseresult':

        confirmation = query_yes_no('Are you sure you want to release the result with id ' + parsed.rid)
        if confirmation:
            print 'Release result ' + parsed.rid + '....'
            update_result(parsed.rid, 'Released')

    if parsed.sub_commands == 'rejectresult':

        confirmation = query_yes_no('Are you sure you want to reject the result with id ' + parsed.rid)
        if confirmation:
            print 'Reject result ' + parsed.rid + '....'
            update_result(parsed.rid, 'Rejected')

    if parsed.sub_commands == 'stoprunning':
        confirmation = query_yes_no('Are you sure you want to stop all the running capsules?')
        if confirmation:
            print 'Stopping all the running VMs'
            stop_running_vms()

    if parsed.sub_commands == 'approvefullaccess':
        confirmation = query_yes_no('Are you sure you want to give approval for full access to VM ' + parsed.vm + '?')
        if confirmation:
            print 'Giving full access for  VM ' + parsed.vm + '....'
            update_vmtype(parsed.vm, parsed.vmuser, 'true')

    if parsed.sub_commands == 'rejectfullaccess':
        confirmation = query_yes_no('Are you sure you want to reject full access to VM ' + parsed.vm + '?')
        if confirmation:
            print 'Rejecting full access for  VM ' + parsed.vm + '....'
            update_vmtype(parsed.vm, parsed.vmuser, 'false')

    if parsed.sub_commands == 'showcapsules':
        print 'Showing capsules information for user ' + parsed.vmuser + '....'
        show_capsules(parsed.vmuser)

    if parsed.sub_commands == 'showpendingfullaccess':
        print 'Showing capsules list which has pending requests for full data access. Username: ' + parsed.vmuser + '....'
        show_pending_fullaccess(parsed.vmuser)
