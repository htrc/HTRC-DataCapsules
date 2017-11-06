# Add the required public key to the root user - this can be replaced at ../scripts/authorized_keys file
mkdir /root/.ssh
cp /home/$SSH_USERNAME/uploads/root_authorized_keys /root/.ssh/authorized_keys
chown -R root /root/.ssh
chmod 700 /root/.ssh
chmod 600 /root/.ssh/authorized_keys

# enable logging of root user activity
cat  /home/$SSH_USERNAME/uploads/enableSyslogging >> /root/.bashrc

# Removing the password for the DCUSER
cp /home/$SSH_USERNAME/uploads/dcuserSudoAccessRestrictions /etc/sudoers.d/dcuserSudoAccessRestrictions
chmod 0440 /etc/sudoers.d/dcuserSudoAccessRestrictions
passwd -d $SSH_USERNAME

# Removing password authentication for SSH for the DC
sed -i -e 's/#PasswordAuthentication\syes/PasswordAuthentication no/g' /etc/ssh/sshd_config
