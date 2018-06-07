#!/bin/bash


SSH_USERNAME=$(whoami)

echo "$SSH_USERNAME" | sudo -S cp /home/$SSH_USERNAME/uploads/org.freedesktop.login1.policy /usr/share/polkit-1/actions/org.freedesktop.login1.policy
echo "Command : sudo -S cp /home/$SSH_USERNAME/uploads/org.freedesktop.login1.policy /usr/share/polkit-1/actions/org.freedesktop.login1.policy"

echo "$SSH_USERNAME" | sudo -S cp /home/$SSH_USERNAME/uploads/nodpms.desktop /etc/xdg/autostart/nodpms.desktop
echo "Command : sudo -S cp /home/$SSH_USERNAME/uploads/nodpms.desktop /etc/xdg/autostart/nodpms.desktop"

echo "$SSH_USERNAME" | sudo -S cp /home/$SSH_USERNAME/uploads/lightdm.conf /etc/lightdm/lightdm.conf
echo "Command : sudo -S cp /home/$SSH_USERNAME/uploads/lightdm.conf /etc/lightdm/lightdm.conf"

echo "$SSH_USERNAME" | sudo -S mkdir -p /etc/gdm
echo "Command : sudo -S mkdir -p /etc/gdm"

echo "$SSH_USERNAME" | sudo -S cp /home/$SSH_USERNAME/uploads/custom.conf /etc/gdm/custom.conf
echo "Command : sudo -S cp /home/$SSH_USERNAME/uploads/custom.conf /etc/gdm/custom.conf"


# Add the required public key to the root user - this can be replaced at ../scripts/authorized_keys file
echo "$SSH_USERNAME" | sudo -S mkdir -p /root/.ssh
echo "$SSH_USERNAME" | sudo -S cp /home/$SSH_USERNAME/uploads/root_authorized_keys /root/.ssh/authorized_keys
echo "$SSH_USERNAME" | sudo -S chown -R root /root/.ssh
echo "$SSH_USERNAME" | sudo -S chmod 700 /root/.ssh
echo "$SSH_USERNAME" | sudo -S chmod 600 /root/.ssh/authorized_keys


# enable logging of root user activity
echo "$SSH_USERNAME" | cat  /home/$SSH_USERNAME/uploads/enableSyslogging >> sudo -S /root/.bashrc


# Removing the UI password prompt
echo "$SSH_USERNAME" | sudo -S cp /home/$SSH_USERNAME/uploads/99-nouipassword.pkla /etc/polkit-1/localauthority/50-local.d/99-nouipassword.pkla


# Removing password authentication for SSH for the DC
echo "$SSH_USERNAME" | sudo -S sed -i -e 's/#PasswordAuthentication\syes/PasswordAuthentication no/g' /etc/ssh/sshd_config

export DISPLAY=:0
echo "$SSH_USERNAME" | sudo -S dbus-launch gsettings set org.gnome.desktop.session idle-delay 0
echo "$SSH_USERNAME" | sudo -S dbus-launch gsettings set org.gnome.desktop.screensaver lock-enabled false
echo "$SSH_USERNAME" | sudo -S dbus-launch gsettings set org.gnome.desktop.lockdown disable-lock-screen true
