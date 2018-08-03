#!/bin/bash

apt-get install -y  nginx

ufw allow 'Nginx HTTP'

cp -rTf /tmp/uploads/nginxConf /etc/nginx/conf.d
cp -rTf /tmp/uploads/nginxSSL /etc/nginx/ssl

systemctl restart nginx

