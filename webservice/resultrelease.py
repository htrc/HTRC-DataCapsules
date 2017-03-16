#!/usr/bin/python

import MySQLdb
import os
import cmd
import sys
import datetime
import smtplib
import logging

from email.MIMEText import MIMEText

######
DB_HOST = "localhost"
DB_USER = "htrc"
DB_PWD = "htrcsecurecapsule"
DB_NAME = "htrcvirtdb"
SMTP_HOST = "mail-relay.iu.edu"
SMTP_PORT = 465
SMTP_USER = "xxxxx"
SMTP_PWD = "xxxxxx"


DOWNLOAD_URL = "https://chinkapin.pti.indiana.edu/download?randomid={0}"

REVIEWER_EMAILS=['user@indiana.edu']
######

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
handler = logging.FileHandler('result-release.log')
handler.setLevel(logging.INFO)
logger.addHandler(handler)

class ResultRelease(cmd.Cmd):

    def __init__(self):
        cmd.Cmd.__init__(self)
        self.prompt = '> '

        self.db = MySQLdb.connect(DB_HOST, DB_USER, DB_PWD, DB_NAME)
        self.cursor = self.db.cursor()
        self.email_content_template = "Dear {0}, \n\nThank you for using HTRC Data Capsule! " + \
            "You can download your result from the link below. \n{1}"

    def _query_sql_(self, sql):
        try:
           self.cursor.execute(sql)
           results = self.cursor.fetchall()
           for row in results:
              vmid = row[0]
              rid = row[1]
              createtime = row[2]
              email = row[3]
              print("vmid={0}, rid={1}, email={2}, createtime={3}".format(vmid, rid, email, createtime))
        #except:
        except Exception, ex:
           print str(ex)
           print "Error: unable to fecth data"

    def _send_email_(self, content, subject, destination):
	msg = MIMEText(content, 'plain')
        msg['Subject'] = subject
#             msg['From'] = sender # some SMTP servers will do this automatically, not all

#        conn = smtplib.SMTP(SMTP_HOST)
        print("content:{0}, subject:{1}, destination:{2}".format(content, subject, destination))
        conn = smtplib.SMTP_SSL(SMTP_HOST, SMTP_PORT)
        conn.set_debuglevel(True)
        #conn.ehlo()
        #conn.starttls()
        #conn.ehlo()
        conn.login(SMTP_USER, SMTP_PWD)
        try:
            conn.sendmail('sharc@indiana.edu', destination, msg.as_string())
        finally:
            conn.close()
            print "Email sent"

    def _write_file_(self, data, filename):
    	with open(filename, 'wb') as f:
        	f.write(data)

    def _write_zip_file_(self, zipcontent, zipfilepath):
        """ function that writes text to a zip file

        arguments:
        zipcontent -- text returned from Data API
        zipfilepath -- zip file name to be written
        """

        # open file to write
        zf = zipfile.ZipFile(zipfilepath, mode='w')

        # read from zip stream
        zippedFile = zipfile.ZipFile(zipcontent, "r")
        try:
            # getting a list of entries in the ZIP
            infoList = zippedFile.infolist()
            for zipInfo in infoList:
                entryName = zipInfo.filename
                entry = zippedFile.open(entryName, "r")

                # read zip entry content
                content = ''
                line = entry.readline()
                while (line != ""):
                    line = entry.readline()
                    content += line

                # remember to close each entry
                entry.close()

                # write to zip file in disk
                zf.writestr(zipInfo, content)

        finally:
            print 'Closing zip file'
            zf.close()
            zippedFile.close()

    def do_show_unrelease(self, args):
        sql = "select results.vmid, results.resultid, results.createtime, users.useremail from results,vms,users where results.vmid=vms.vmid and vms.username=users.username and results.notified='NO'"
        self._query_sql_(sql)

    def do_show_release(self, args):
        sql = "select results.vmid, results.resultid, results.createtime, users.useremail from results,vms,users where results.vmid=vms.vmid and vms.username=users.username and results.notified='YES'"
        self._query_sql_(sql)

    def do_disable_result(self, line):
        args = line.split()
        if len(args) != 1:
            print "Usage: disable_result <rid>"
            return
        rid = args[0]

        try:
            now = datetime.datetime.now()
            sql = "update results set notified='REJECTED', notifiedtime='{0}' where resultid='{1}'".format(now.strftime('%Y-%m-%d %H:%M:%S'), rid)
            self.cursor.execute(sql)
            self.db.commit()

            # get email
            sql = "select users.username, users.useremail, results.resultid " + \
                "from users, vms, results where users.username=vms.username and " + \
                "vms.vmid=results.vmid and resultid='{0}'".format(rid)

            self.cursor.execute(sql)
            results = self.cursor.fetchall()
            for row in results:
                email = row[1]
                # send notifications to reviewers
                for reviewer_addr in REVIEWER_EMAILS:
                    self._send_email_("Result {0} is not approved and will not be sent to {1}".format(rid, email),\
                    "HTRC Data Capsule Result Disapproval Notification", reviewer_addr)

                    logger.info("Result {0} is not approved and will not be sent to {1}".format(rid, email))
        except Exception, ex:
            print str(ex)
            print "Error: unable to reject result with id " + rid

    def do_release(self, line):
        args = line.split()
        if len(args) != 1:
            print "Usage: release <rid>"
            return
	rid = args[0]

        # check if it has been released or not
        sql = "select * from results where notified='NO' and resultid='{0}'".format(rid)
        try:
           self.cursor.execute(sql)
           results = self.cursor.fetchall()
           if len(results) == 0:
               print "Result with id {0} has been released. \n".format(rid)
               return
        except Exception, ex:
           print str(ex)
           print "Error: unable to fecth result with id " + rid
           return

        # send emails
        sql = "select users.username, users.useremail, results.resultid " + \
            "from users, vms, results where users.username=vms.username and " + \
            "vms.vmid=results.vmid and resultid='{0}'".format(rid)
        try:
            self.cursor.execute(sql)
            results = self.cursor.fetchall()
            for row in results:
                username = row[0]
                email = row[1]
                id = row[2]
                print("username={0}, email={1}, id={2}".format(username, email, id))
#                self._send_email_("Dear {0}, \n\nThank you for using HTRC Data Capsule! " + \
#            "You can download your result from the link below. \n{1}".format(username, DOWNLOAD_URL.format(rid)),\
#                    "HTRC Data Capsule Result Download URL", email)
                self._send_email_(self.email_content_template.format(username, DOWNLOAD_URL.format(rid)),\
                    "HTRC Data Capsule Result Download URL", email)

		# mark it as released
                now = datetime.datetime.now()
                sql = "update results set notified='YES', notifiedtime='{0}' where resultid='{1}'".format(now.strftime('%Y-%m-%d %H:%M:%S'), rid)
                self.cursor.execute(sql)
                self.db.commit()

                # send notifications to reviewers
                for reviewer_addr in REVIEWER_EMAILS:
                    self._send_email_("Result {0} has been sent to {1} with download URL {2}".format(id, email, DOWNLOAD_URL.format(rid)),\
                    "HTRC Data Capsule Result Release Notification", reviewer_addr)

                logger.info("Result {0} has been sent to {1} with download URL {2}".format(id, email, DOWNLOAD_URL.format(rid)))
        except Exception as ex:
            print ex
            print "Error: unable to send email\n"

    def do_get(self, line):
        args = line.split(' ')
        if len(args) != 2:
            print "Usage: get <rid> </path/to/filename.zip>"
            return
        rid = args[0]
        filename = args[1]

        if not os.path.exists(os.path.dirname(filename)):
            os.makedirs(os.path.dirname(filename))

        sql = "select datafield from results where resultid='{0}'".format(rid)
        print args
        try:
            self.cursor.execute(sql)
            results = self.cursor.fetchall()
            cnt = 0
            for row in results:
                self._write_file_(row[0], filename)
                cnt = cnt + 1
            print "finish dowload ", cnt
        except Exception, ex:
           print str(ex)
           print "Error: unable to fecth data"

    def do_quit(self, args):
        self.db.close()
        sys.exit(1)

if __name__ == '__main__':
    print \
        "*********************************************************** \n", \
        "**      HTRC Data Capsules Result Release Tool v1.0      ** \n", \
        "*********************************************************** \n", \
        "**  Usage:                                               ** \n", \
        "**    quit: exit this  terminal                          ** \n", \
        "**    help: show available commands                      ** \n", \
        "**    show_release: show results have been released      ** \n", \
        "**    show_unrelease: show results pending for release   ** \n", \
        "**    release <rid>: release the result with <rid>       ** \n", \
        "**    disable_result <rid>: disable the result with <rid>** \n", \
        "**    get <rid> <filename.zip>: get the result with <rid>** \n", \
        "**      and save to path </path/to/filename.zip>         ** \n", \
        "*********************************************************** \n"
    ResultRelease().cmdloop()