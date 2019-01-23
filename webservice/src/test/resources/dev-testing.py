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
DC_API = '<dc_api_host>'
keyFile = '<key_fle>'
certFile = '<cert_file>'
bearer_token = '<token>'

def stop_vm(vmid, username, useremail):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

    params = urllib.urlencode({'vmid': vmid})

    # POST the request
    conn = httplib.HTTPConnection(DC_API)
    conn.request("POST", '/sloan-ws/stopvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data


def start_vm(vmid, username, useremail):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

    params = urllib.urlencode({'vmid': vmid})

    # POST the request
    conn = httplib.HTTPConnection(DC_API)
    conn.request("POST", '/sloan-ws/launchvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data


def switch_vm(vmid, username, useremail, mode):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

    params = urllib.urlencode({'vmid': vmid, 'mode': mode})

    # POST the request
    conn = httplib.HTTPConnection(DC_API)
    conn.request("POST", '/sloan-ws/switchvm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data




def show_release():
    headers = {'Content-Type': 'application/json'}

    # GET the request
    conn = httplib.HTTPConnection(DC_API)
    conn.request("GET", '/sloan-ws/showreleased')
    response = conn.getresponse()

    data = response.read()
    parsed = json.loads(data)
    print json.dumps(parsed, indent=4, sort_keys=True)


def show_unrelease():
    headers = {'Content-Type': 'application/json'}

    # GET the request
    conn = httplib.HTTPConnection(DC_API)
    conn.request("GET", '/sloan-ws/showunreleased')
    response = conn.getresponse()

    data = response.read()
    parsed = json.loads(data)
    print json.dumps(parsed, indent=4, sort_keys=True)


def retrieve_file(result_id, out_file):
    # GET the request
    conn = httplib.HTTPConnection(DC_API)
    conn.request("GET", '/sloan-ws/retrieveresultfile?randomid=' + result_id)
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
    conn = httplib.HTTPConnection(DC_API)
    conn.request("GET", '/sloan-ws/download?randomid=' + result_id)
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
    conn = httplib.HTTPConnection(DC_API)
    conn.request("POST", '/sloan-ws/updateresult', params)
    response = conn.getresponse()

    print response.read()


def stop_running_vms():
    headers = {'Content-Type': 'application/json',
               'Accept': 'application/json'}

    # Get request
    conn = httplib.HTTPConnection(DC_API)
    conn.request("GET", '/sloan-ws/listvms')
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
    conn = httplib.HTTPConnection(DC_API)
    conn.request("POST", '/sloan-ws/updatevm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data


def create_vm(imagename, loginusername, loginpassword, memory, vcpu, type):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'Authorization': bearer_token}

    print ("==================================================")
    print ("Capsule creation")
    print ("----------------")

    params = urllib.urlencode(
        {'imagename': imagename, 'loginusername': loginusername, 'loginpassword': loginpassword, 'memory': memory,
         'vcpu': vcpu, 'type': type})
    conn = httplib.HTTPSConnection(DC_API, key_file=keyFile, cert_file=certFile)
    conn.request("POST", '/createvm', params, headers)
    response = conn.getresponse()

    if response.status != 200:
        sys.exit('ERROR : Capsule creation failed - ' + response.read())

    vmid = json.loads(response.read())['vmid']
    print ("Capsule ID : " + vmid)
    state = status_changed('state', vmid, 'CREATE_PENDING')

    if(state == 'ERROR'):
        sys.exit('ERROR : Capsule creation failed!')

    print('Capsule created successfully!')
    print ("==================================================")
    return vmid


def delete_vm(vmid):

    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'Authorization': bearer_token}

    print ("==================================================")
    print ("Capsule deletion vmid : " + vmid)
    print ("--------------------------------------------------")

    params = urllib.urlencode({'vmid': vmid})
    conn = httplib.HTTPSConnection(DC_API, key_file=keyFile, cert_file=certFile)
    conn.request("POST", '/deletevm', params, headers)
    response = conn.getresponse()

    if response.status != 200:
        sys.exit('ERROR : Capsule deletion failed - ' + response.read())

    print('Capsule deleted successfully!')
    print ("==================================================")


def show_capsules():
    headers = {'Authorization': bearer_token}

    print ("==================================================")
    print ("Capsule list")
    print ("------------")

    conn = httplib.HTTPSConnection(DC_API, key_file=keyFile, cert_file=certFile)
    conn.request("POST", '/show', "", headers)
    response = conn.getresponse()

    if response.status != 200:
        sys.exit('ERROR : Capsule retrieval failed - ' + response.read())

    data = response.read()
    parsed = json.loads(data)
    print json.dumps(parsed, indent=4, sort_keys=True)
    print ("==================================================")


def status_changed(param, vmid, current_state):

    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'Authorization': bearer_token}

    params = urllib.urlencode({'vmid': vmid})
    status = current_state

    while status == current_state:
        conn = httplib.HTTPSConnection(DC_API, key_file=keyFile, cert_file=certFile)
        conn.request("POST", '/show', params, headers)
        response = conn.getresponse()
        status = json.loads(response.read())['status'][0][param]
        sys.stdout.write('.')
        sys.stdout.flush()
        time.sleep(5)
    print('')

    return status



if __name__ == '__main__':

    vmid = create_vm('ubuntu-16-04', 'abcdABCD', 'abcdABCD', '2048', '2', 'DEMO')
    show_capsules()
    delete_vm(vmid)


