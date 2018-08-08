#!/bin/bash

apt-get install -y  nginx

ufw allow 'Nginx HTTP'

rm /etc/nginx/sites-enabled/default
cp -rTf /tmp/guest_uploads/nginxConf /etc/nginx/conf.d
cp -rTf /tmp/guest_uploads/nginxSSL /etc/nginx/ssl

systemctl restart nginx

