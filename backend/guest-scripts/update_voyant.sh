#!/bin/bash

SSH_USERNAME=$1

cd /opt/applications

wget https://analytics.hathitrust.org/files/voyant.zip
unzip voyant.zip
rm /home/$SSH_USERNAME/Desktop/voyant.desktop
cp VoyantServer2_4-M7/voyant.desktop /home/$SSH_USERNAME/Desktop/
chown $SSH_USERNAME:$SSH_USERNAME /home/$SSH_USERNAME/Desktop/voyant.desktop
chown -R $SSH_USERNAME:$SSH_USERNAME /opt/applications/VoyantServer2_4-M7


