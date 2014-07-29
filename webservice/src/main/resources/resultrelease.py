#!/usr/bin/python

import MySQLdb
import cmd
import sys
import datetime  
import smtplib
from email.MIMEText import MIMEText

######
DB_HOST = ""
DB_USER = ""
DB_PWD = ""
DB_NAME = ""
SMTP_HOST = ""
SMTP_PORT = 587
SMTP_USER = ""
SMTP_PWD = ""

DOWNLOAD_URL = ""
######

class ResultRelease(cmd.Cmd):
     
    def __init__(self):
        cmd.Cmd.__init__(self)
        self.prompt = '> '
         
        self.db = MySQLdb.connect(DB_HOST, DB_USER, DB_PWD, DB_NAME)
        self.cursor = self.db.cursor()
        self.email_content_template = "Dear {}, \n\nThank you for using HTRC Data Capsule! " + \
            "You can download your result from the link below. \n{}"
    
    def _query_sql_(self, sql):        
        try:
           self.cursor.execute(sql)
           results = self.cursor.fetchall()
           for row in results:
              vmid = row[0] 
              rid = row[1]
              starttime = row[3]
              print("vmid={}, rid={}, starttime={}".format(vmid, rid, starttime))
        except:
           print "Error: unable to fecth data"
           
    def _send_email_(self, content, subject, destination):
        msg = MIMEText(content, 'plain')
        msg['Subject'] = subject
        msg['From'] = SMTP_USER
    
        conn = smtplib.SMTP(SMTP_HOST, SMTP_PORT)
        conn.set_debuglevel(False)
        conn.starttls()
        conn.login(SMTP_USER, SMTP_PWD)
        try:
            conn.sendmail(SMTP_USER, destination, msg.as_string())
        finally:
            conn.close()        
         
    def do_show_unrelease(self, args):
        sql = "select * from results where notified=0"
        self._query_sql_(sql)
        
    def do_show_release(self, args):
        sql = "select * from results where notified=1"
        self._query_sql_(sql)
        
    def do_release(self, args):
        rid = args
        
        # check if it has been released or not
        
        # update starttime
        now = datetime.datetime.now()
        sql = "update results set starttime='{}' where resultid='{}'". \
            format(now.strftime('%Y-%m-%d %H:%M:%S'), rid)
        try:
            self.cursor.execute(sql)
            self.db.commit()
        except:
            print "Error: uable to update time stamp of record"
            self.db.rollback()
            return
        
        # send email
        sql = "select users.username, users.useremail, results.resultid " + \
            "from users, uservm, results where users.username=uservm.username and " + \
            "uservm.vmid=results.vmid and resultid='{}'".format(rid)
        try:
            self.cursor.execute(sql)
            results = self.cursor.fetchall()
            for row in results:
                username = row[0]
                email = row[1]
                id = row[2]
                print("username={}, email={}, id={}".format(username, email, id))
                self._send_email_(self.email_content_template.format(username, DOWNLOAD_URL.format(rid)),\
                    "HTRC Data Capsule Result Download URL", email)
                
                # mark it as released
                sql = "update results set notified=1 where resultid='{}'".format(rid)
                self.cursor.execute(sql)
                self.db.commit()
        except Exception, ex:
            #print str(ex)
            print "Error: unable to send email\n"        
        
    def do_quit(self, args):
        self.db.close()
        sys.exit(1)
 
if __name__ == '__main__':    
    ResultRelease().cmdloop()
