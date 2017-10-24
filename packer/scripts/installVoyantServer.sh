cd /tmp
apt-get install unzip
wget https://github.com/sgsinclair/VoyantServer/releases/download/2.4.0-M1/VoyantServer2_4-M1.zip 
unzip VoyantServer2*.zip
mv VoyantServer2_4-M1 /opt/VoyantServer
chown -R $SSH_USERNAME /opt/VoyantServer
