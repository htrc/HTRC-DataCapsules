# Go to temp directory
cd /tmp 

# You can change what anaconda version you want at 
# https://repo.continuum.io/archive/
wget https://repo.continuum.io/archive/Anaconda2-4.2.0-Linux-x86_64.sh

bash Anaconda2-4.2.0-Linux-x86_64.sh -b -p /opt/anaconda
chown -R $SSH_USERNAME /opt/anaconda

cp $USER_HOME_DIR/uploads/anaconda.sh /etc/profile.d/anaconda.sh
source /etc/profile.d/anaconda.sh

#cleanup
rm Anaconda2-4.2.0-Linux-x86_64.sh


