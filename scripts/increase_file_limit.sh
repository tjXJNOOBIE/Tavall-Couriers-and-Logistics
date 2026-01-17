#!/bin/bash
# ---------------------------------------------------------
# TAVALL COURIERS - UBUNTU 24.04 (MANUAL RUN EDITION)
# ---------------------------------------------------------
# Purpose: Tunes limits for direct terminal execution (java -jar ...)
# Must run as root (sudo)

if [ "$EUID" -ne 0 ]; then 
  echo "Please run as root (sudo)"
  exit
fi

echo ">>> TUNING FOR MANUAL EXECUTION..."

# 1. KERNEL LEVEL (The Ceiling)
# This sets the maximum number of open files for the ENTIRE OS.
# We use a dedicated file in sysctl.d to avoid messing up defaults.
echo ">>> Setting Kernel Max Files..."
cat <<EOF > /etc/sysctl.d/99-tavall-couriers.conf
fs.file-max = 2097152
EOF

# Apply kernel changes immediately
sysctl --system > /dev/null

# 2. USER SESSION LIMITS (The Gates)
# This configures PAM (Pluggable Authentication Modules).
# When you log in (SSH or Terminal), these limits apply to your shell.
echo ">>> Setting User Session Limits..."
cat <<EOF > /etc/security/limits.d/99-tavall-couriers.conf
* soft    nofile  1048576
* hard    nofile  1048576
root    soft    nofile  1048576
root    hard    nofile  1048576
EOF

# 3. ENABLE PAM LIMITS MODULE
# Ensure the OS actually READS the file above when you log in.
echo ">>> Ensuring PAM loads limits..."
if ! grep -q "pam_limits.so" /etc/pam.d/common-session; then
    echo "session required pam_limits.so" >> /etc/pam.d/common-session
    echo "Added pam_limits.so to common-session."
else
    echo "pam_limits.so already active. Good."
fi

# 4. SYSTEMD USER SESSION (The Ubuntu 24 Specific Fix)
# Even if you don't run as a "service", Ubuntu 24 logs you in via systemd-logind.
# We need to ensure your user slice allows high limits.
echo ">>> Tuning User Login Slice..."
mkdir -p /etc/systemd/user.conf.d
cat <<EOF > /etc/systemd/user.conf.d/99-tavall-limits.conf
[Manager]
DefaultLimitNOFILE=1048576
EOF

echo "---------------------------------------------------------"
echo "DONE. CRITICAL NEXT STEPS:"
echo "1. You MUST disconnect and log back in (SSH/Terminal) for this to take effect."
echo "2. After logging back in, type: 'ulimit -n'"
echo "   -> If it says 1048576, you are ready."
echo "   -> If it says 1024, the tuning failed."
echo "---------------------------------------------------------"