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
#             msg['From'] = sender # some SMTP servers will do this automatically, not all
        
        conn = smtplib.SMTP(SMTP_HOST)
        conn.set_debuglevel(True)
        conn.starttls()
        conn.login(SMTP_USER, SMTP_PWD)
        try:
            conn.sendmail('htrccapsule@gmail.com', destination, msg.as_string())
        finally:
            conn.close()             
 
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
        sql = "select * from results where notified=0"
        self._query_sql_(sql)
        
    def do_show_release(self, args):
        sql = "select * from results where notified=1"
        self._query_sql_(sql)

    def do_disable_result(self, line):
        args = line.split()
        if len(args) != 1:
            print "Usage: disable_result <rid>"
            return
        rid = args[0]
        
        try:
            sql = "update results set notified=-1 where resultid='{}'".format(rid)
            self.cursor.execute(sql)
            self.db.commit()
        except Exception, ex:
            print str(ex)
            print "Error: unable to disable result with id " + rid
        
    def do_release(self, line):
        args = line.split()
        if len(args) != 1:
            print "Usage: release <rid>"
            return
    rid = args[0]        
        
        # check if it has been released or not
        sql = "select * from results where notified=0 and resultid='{}'".format(rid)
        try:
           self.cursor.execute(sql)
           results = self.cursor.fetchall()
           if len(results) == 0:
               print "Result with id {} has been released. \n".format(rid)
               return
        except Exception, ex:
           print str(ex)
           print "Error: unable to fecth result with id " + rid
           return
        
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

    def do_get(self, line):
        args = line.split(' ')
        if len(args) != 2:
            print "Usage: get <rid> </path/to/filename.zip>"
            return
        rid = args[0]
        filename = args[1]

        if not os.path.exists(os.path.dirname(filename)):
            os.makedirs(os.path.dirname(filename))

        sql = "select datafield from results where resultid='{}'".format(rid)
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