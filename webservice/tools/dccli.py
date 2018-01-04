import os
import sys
import json
import urllib
import urllib2
import argparse
import string
import httplib


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
    'htrc-remote-user-email': useremail }

    params = urllib.urlencode({'vmid': vmid})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan-ws/deletevm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data

def stop_vm(vmid, username, useremail):

    headers = {'Content-Type': 'application/x-www-form-urlencoded',
    'htrc-remote-user': username,
    'htrc-remote-user-email': useremail }

    params = urllib.urlencode({'vmid': vmid})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan-ws/stopvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data

def start_vm(vmid, username, useremail):

    headers = {'Content-Type': 'application/x-www-form-urlencoded',
    'htrc-remote-user': username,
    'htrc-remote-user-email': useremail }

    params = urllib.urlencode({'vmid': vmid})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan-ws/launchvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data

def switch_vm(vmid, username, useremail, mode):

    headers = {'Content-Type': 'application/x-www-form-urlencoded',
    'htrc-remote-user': username,
    'htrc-remote-user-email': useremail }

    params = urllib.urlencode({'vmid': vmid, 'mode': mode})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan-ws/switchvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data

def create_vm(username, useremail, imagename, loginusername, loginpassword, memory, vcpu):

    headers = {'Content-Type': 'application/x-www-form-urlencoded',
    'htrc-remote-user': username,
    'htrc-remote-user-email': useremail }

    params = urllib.urlencode({'imagename': imagename, 'loginusername': loginusername, 'loginpassword': loginpassword, 'memory': memory, 'vcpu': vcpu})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan-ws/createvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data

def show_release():

    headers = {'Content-Type': 'application/json'}

    # GET the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/sloan-ws/showreleased')
    response = conn.getresponse()

    data = response.read()
    parsed = json.loads(data)
    print json.dumps(parsed, indent=4, sort_keys=True)

def show_unrelease():

    headers = {'Content-Type': 'application/json'}

    # GET the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/sloan-ws/showunreleased')
    response = conn.getresponse()

    data = response.read()
    parsed = json.loads(data)
    print json.dumps(parsed, indent=4, sort_keys=True)


def retrieve_file(result_id):

    # GET the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/sloan-ws/retrieveresultfile?randomid=' + result_id)
    response = conn.getresponse()

    if(response.status != 200):
        print response.read()
    else:
        data = response.read()
        f = open('results.zip','w')
        f.write(data)
        f.close()
        print 'Result written to results.zip file...'

def download_file(result_id):

    # GET the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/sloan-ws/download?randomid=' + result_id)
    response = conn.getresponse()

    if(response.status != 200):
        print response.read()
    else:
        data = response.read()
        f = open('results.zip','w')
        f.write(data)
        f.close()
        print 'Result downloaded to results.zip file...'

def update_result(result_id, status):

    params = urllib.urlencode({'resultid': result_id, 'status': status})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan-ws/updateresult', params)
    response = conn.getresponse()

    print response.read()


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

    downloadfile = subparsers.add_parser('downloadfile', description='Download result file')
    downloadfile.add_argument('rid')

    releaseresult = subparsers.add_parser('releaseresult', description='Release the result')
    releaseresult.add_argument('rid')
    
    rejectresult = subparsers.add_parser('rejectresult', description='Reject the result')
    rejectresult.add_argument('rid') 

    parsed = parser.parse_args()

    if parsed.sub_commands == 'delete':
        confirmation = query_yes_no('Are you sure you want to delete the VM ' + parsed.vm + '? This operation is not recoverable.')
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
        confirmation = query_yes_no('Are you sure you want to switch the VM ' + parsed.vm + ' to ' + parsed.mode + ' mode?')
        if confirmation:
            print 'Switching  VM ' + parsed.vm + ' to ' + parsed.mode + '....'
            switch_vm(parsed.vm, parsed.vmuser, parsed.vmuseremail, parsed.mode)

    if parsed.sub_commands == 'create':
        confirmation = query_yes_no('Are you sure you want to create a VM with ' + parsed.imagename + ',' + parsed.memory + ',' + parsed.vcpu + ' ?')
        if confirmation:
            print 'Creating  VM with image:' + parsed.imagename + ', VNC User name:' + parsed.vncusername + ', VNC Password:' + parsed.vncpassword + ', memory: '+ parsed.memory + ', vcpu: ' + parsed.vcpu + '...'
            create_vm(parsed.vmuser, parsed.vmuseremail, parsed.imagename, parsed.vncusername, parsed.vncpassword, parsed.memory, parsed.vcpu)

    if parsed.sub_commands == 'showrelease':
        print 'Released Results:'
        show_release()

    if parsed.sub_commands == 'showunrelease':
        print 'Un-Released Results:'
        show_unrelease()

    if parsed.sub_commands == 'retrievefile':    
        confirmation = query_yes_no('Are you sure you want to retrieve file with id ' + parsed.rid )
        if confirmation:
            print 'Retrieving file for result ' + parsed.rid + '....'
            retrieve_file(parsed.rid)

    if parsed.sub_commands == 'downloadfile':
    
        confirmation = query_yes_no('Are you sure you want to download file with id ' + parsed.rid )
        if confirmation:
            print 'Download file for result ' + parsed.rid + '....'
            download_file(parsed.rid)

    if parsed.sub_commands == 'releaseresult':
    
        confirmation = query_yes_no('Are you sure you want to release the result with id ' + parsed.rid )
        if confirmation:
            print 'Release result ' + parsed.rid + '....'
            update_result(parsed.rid, 'Released')

    if parsed.sub_commands == 'rejectresult':
    
        confirmation = query_yes_no('Are you sure you want to reject the result with id ' + parsed.rid )
        if confirmation:
            print 'Reject result ' + parsed.rid + '....'
            update_result(parsed.rid, 'Rejected')

