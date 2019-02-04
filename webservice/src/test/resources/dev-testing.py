import os
import sys
import json
import urllib
import urllib2
import argparse
import string
import httplib
import time
import pandas

# DC
DC_API = 'localhost'
PORT = '8080'


def stop_vm(vmid, username, useremail):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

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
               'htrc-remote-user-email': useremail}

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
               'htrc-remote-user-email': useremail}

    params = urllib.urlencode({'vmid': vmid, 'mode': mode})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan-ws/switchvm', params, headers)
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


def retrieve_file(result_id, out_file):
    # GET the request
    conn = httplib.HTTPConnection(DC_API, PORT)
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
    conn = httplib.HTTPConnection(DC_API, PORT)
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
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan-ws/updateresult', params)
    response = conn.getresponse()

    print response.read()


def stop_running_vms():
    headers = {'Content-Type': 'application/json',
               'Accept': 'application/json'}

    # Get request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("GET", '/sloan-ws/listvms')
    response = conn.getresponse()

    if response.status == 200:
        vms = json.loads(response.read())['vmsInfo']

        for vm in vms:
            if vm["vmState"] == "RUNNING":
                print 'Stopping VM: {}'.format(vm["vmid"])
                stop_vm(vm["vmid"], vm["username"], vm["userEmail"])
                time.sleep(5)


def update_vmtype_for_sharees(vmid, username, status, guids):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    print ("==================================================")
    print ("Update full access status of " + guids + " to " + status + " in : " + vmid)
    print ("--------------------------------------------------")

    params = urllib.urlencode({'vmId': vmid, 'type': 'RESEARCH-FULL', 'full_access': status, 'guids' : guids})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan/updatevm', params, headers)
    response = conn.getresponse()

    data = response.read()
    #print data
    show_shared_capsules()
    print ("==================================================\n\n")


def accept_tou(vmid, username):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    print ("==================================================")
    print ("User accepts TOU : " + username)
    print ("--------------------------------------------------")

    params = urllib.urlencode({'vmId': vmid, 'tou': 'true'})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan/updateusertou', params, headers)
    response = conn.getresponse()

    data = response.read()
    #print data
    show_shared_capsules()
    print ("==================================================\n\n")


def add_sharees(vmid, username, sharees):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    print ("==================================================")
    print ("Add sharees( " + sharees + " ) to capsule capsule : " + vmid)
    print ("--------------------------------------------------")

    params = urllib.urlencode({'vmId': vmid, 'sharees': sharees})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan/addsharees', params, headers)
    response = conn.getresponse()

    data = response.read()
    #print data
    show_shared_capsules()
    print ("==================================================\n\n")


def update_vmtype(vmid, username, status):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    print ("==================================================")
    print ("Grant/Reject full access( " + status + " ) for capsule : " + vmid)
    print ("--------------------------------------------------")

    params = urllib.urlencode({'vmId': vmid, 'type': 'RESEARCH-FULL', 'full_access': status})

    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan/updatevm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data
    show_shared_capsules()
    print ("==================================================\n\n")


def update_vm_request(vmid, username):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    print ("==================================================")
    print ("Request full access for capsule : " + vmid)
    print ("--------------------------------------------------")

    params = urllib.urlencode({'vmId': vmid, 'type': 'RESEARCH', 'full_access': 'false',
                               'desc_requirement':'req1', 'consent':'true', 'title':'title1'
                                  , 'rr_data_files':'data_file1', 'rr_result_usage':'rr_usage1'
                                  , 'desc_nature':'desc_nature1', 'desc_outside_data':'outside_data1'
                                  , 'desc_links':'links1'})
    # POST the request
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan/updatevm', params, headers)
    response = conn.getresponse()

    data = response.read()

    print data

    show_shared_capsules()
    print ("==================================================\n\n")


def create_vm(username, useremail, imagename, loginusername, loginpassword, memory, vcpu, type):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username,
               'htrc-remote-user-email': useremail}

    print ("==================================================")
    print ("Capsule creation")
    print ("----------------")

    params = urllib.urlencode(
        {'imagename': imagename, 'loginusername': loginusername, 'loginpassword': loginpassword, 'memory': memory,
         'vcpu': vcpu, 'type': type})
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan/createvm', params, headers)
    response = conn.getresponse()

    if response.status != 200:
        sys.exit('ERROR : Capsule creation failed - ' + response.read())

    vmid = json.loads(response.read())['vmid']
    print ("Capsule ID : " + vmid)
    state = status_changed(username, 'state', vmid, 'CREATE_PENDING')

    if(state == 'ERROR'):
        sys.exit('ERROR : Capsule creation failed!')

    print('Capsule created successfully!')
    show_shared_capsules()
    print ("==================================================\n\n")
    return vmid


def delete_vm(username, vmid):

    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    print ("==================================================")
    print ("Capsule deletion vmid : " + vmid)
    print ("--------------------------------------------------")

    params = urllib.urlencode({'vmid': vmid})
    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan/deletevm', params, headers)
    response = conn.getresponse()

    if response.status != 200:
        sys.exit('ERROR : Capsule deletion failed - ' + response.read())

    print('Capsule deleted successfully!')
    print ("==================================================\n\n")


def show_capsules(username):
    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}
    
    #print ("==================================================")
    #print ("Capsule list")
    #print ("------------")

    conn = httplib.HTTPConnection(DC_API, PORT)
    conn.request("POST", '/sloan/show', "", headers)
    response = conn.getresponse()

    if response.status != 200:
        sys.exit('ERROR : Capsule retrieval failed - ' + response.read())

    data = response.read()
    parsed = json.loads(data)
    #print json.dumps(parsed, indent=4, sort_keys=True)
    #print ("==================================================")
    return parsed


def status_changed(username, param, vmid, current_state):

    headers = {'Content-Type': 'application/x-www-form-urlencoded',
               'htrc-remote-user': username}

    params = urllib.urlencode({'vmid': vmid})
    status = current_state

    while status == current_state:
        conn = httplib.HTTPConnection(DC_API, PORT)
        conn.request("POST", '/sloan/show', params, headers)
        response = conn.getresponse()
        status = json.loads(response.read())['status'][0][param]
        sys.stdout.write('.')
        sys.stdout.flush()
        time.sleep(5)
    print('')

    return status

def show_shared_capsules():
    guids = 'A|B'.split("|")
    pandas.set_option('display.width', 5000)
    pandas.set_option('max_colwidth', 5000)
    for guid in guids:
        resp = show_capsules(guid)['status']
        header_list = ['vmid', 'role', 'vm_tou', 'type', 'full_access', 'users_full_access', 'roles']
        df = pandas.DataFrame(columns=header_list)
        print('------------------user:' + guid + '-------------------------')
        for res in resp:
            roles = res['roles']
            roles_list = ''
            for role in roles:
                roles_list += ' [' + role['guid'] + ' ' + ' | ' +  role['role'] + ' | tou:'  +  str(role['tou']) \
                              + ' | fa:' + ('null' if role['full_access'] == None else str(role['full_access'])) + ' ] '
            df2 = pandas.DataFrame([[ res['vmid'],
                                      res['role'],
                                      str(res['vm_tou']),
                                      res['type'],
                                      ('null' if res['full_access'] == None else str(res['full_access'])) ,
                                      ('null' if res['user_full_access'] == None else str(res['user_full_access'])),
                                      roles_list]], columns=header_list)
            df = df.append(df2, ignore_index=True)
        print(df)
        print('-------------------------------------------------')

if __name__ == '__main__':

    vmid = create_vm('A', 'aaa@umail.iu.edu', 'ubuntu-16-04', 'abcd1234', 'abcd1234', '2048', '2', 'RESEARCH')

    # Test VM creation, Adding sharees, Granting full access and accepting TOU
    update_vm_request(vmid, 'A')
    update_vmtype(vmid, 'A', 'true')
    add_sharees(vmid, 'A', '[{guid:\'B\', email : \'bbb@umail.iu.edu\'}]')
    accept_tou(vmid, 'B')
    update_vmtype_for_sharees(vmid, 'A', 'true', 'B')

    delete_vm('A',vmid)


