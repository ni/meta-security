SUMMARY = "ClamAV virus signature databases for offline installations"
DESCRIPTION = "Pre-downloaded ClamAV virus signature databases for offline installations. \
Downloads current signatures at build time using cvdupdate."
HOMEPAGE = "http://www.clamav.net/index.html"
SECTION = "security"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"

require clamav_${PV}.bb

PN = "clamav-signatures"
PACKAGES = "${PN}"
PACKAGE_ARCH = "all"

DEPENDS = "python3-native python3-pip-native"
RDEPENDS:${PN} = "clamav-freshclam"

do_compile[noexec] = "1"
do_configure[noexec] = "1"
do_install[noexec] = "1"
do_download_signatures[network] = "1"

addtask download_signatures after do_unpack before do_install_signatures
addtask install_signatures after do_download_signatures before do_package

python do_download_signatures() {
    import os
    import subprocess
    
    workdir = d.getVar('WORKDIR')
    staging_bindir_native = d.getVar('STAGING_BINDIR_NATIVE')
    sig_download_dir = os.path.join(workdir, 'signatures')
    cvdupdate_dir = os.path.join(workdir, 'cvdupdate_home')
    
    os.makedirs(sig_download_dir, exist_ok=True)
    os.makedirs(cvdupdate_dir, exist_ok=True)
    
    bb.note("Downloading ClamAV signatures using cvdupdate")
    
    python_bin = os.path.join(staging_bindir_native, 'python3-native', 'python3')
    if not os.path.exists(python_bin):
        python_bin = os.path.join(staging_bindir_native, 'python3')
    if not os.path.exists(python_bin):
        bb.fatal("python3 not found in staging")
    
    try:
        pip_cmd = [python_bin, '-m', 'pip', 'install', '--target', cvdupdate_dir, 'cvdupdate']
        result = subprocess.run(pip_cmd, cwd=workdir, stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, text=True, timeout=180)
        if result.returncode != 0:
            bb.fatal(f"Failed to install cvdupdate: {result.stdout}")
        
        env = os.environ.copy()
        env['PYTHONPATH'] = cvdupdate_dir + ':' + env.get('PYTHONPATH', '')
        env['HOME'] = workdir
        
        config_cmd = [python_bin, '-m', 'cvdupdate', 'config', 'set', '--dbdir', sig_download_dir]
        result = subprocess.run(config_cmd, cwd=workdir, env=env, stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, text=True, timeout=30)
        if result.returncode != 0:
            bb.warn(f"cvdupdate config warning: {result.stdout}")
        
        bb.note("Downloading signatures (this may take several minutes)")
        update_cmd = [python_bin, '-m', 'cvdupdate', 'update', '-V']
        result = subprocess.run(update_cmd, cwd=workdir, env=env, stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, text=True, timeout=900)
        bb.note(result.stdout)
        
        if result.returncode != 0:
            bb.fatal(f"cvdupdate failed with return code {result.returncode}")
        
        if os.path.exists(sig_download_dir):
            all_files = [f for f in os.listdir(sig_download_dir) 
                        if os.path.isfile(os.path.join(sig_download_dir, f))]
            
            if not all_files:
                bb.fatal("No signature files were downloaded")
            
            total_size = 0
            for f in all_files:
                fpath = os.path.join(sig_download_dir, f)
                size = os.path.getsize(fpath)
                total_size += size
                bb.note(f"  {f}: {size / (1024*1024):.2f} MB")
            
            if total_size == 0:
                bb.fatal("All downloaded files are 0 bytes")
            
            bb.note(f"Downloaded {len(all_files)} file(s), total: {total_size / (1024*1024):.2f} MB")
        else:
            bb.fatal(f"Signatures directory not created")
            
    except subprocess.TimeoutExpired as e:
        bb.fatal(f"Operation timed out: {str(e)}")
    except Exception as e:
        bb.fatal(f"Error downloading signatures: {str(e)}")
}

do_install_signatures() {
    install -d ${D}${localstatedir}/lib/clamav
    install -m 0644 ${WORKDIR}/signatures/* ${D}${localstatedir}/lib/clamav/ || bbfatal "Failed to install signatures"
}

FILES:${PN} = "${localstatedir}/lib/clamav/*.cvd \
               ${localstatedir}/lib/clamav/*.cld \
               ${localstatedir}/lib/clamav/*.cud \
               ${localstatedir}/lib/clamav/*.cdiff \
               ${localstatedir}/lib/clamav/*.sign \
               ${localstatedir}/lib/clamav/*.txt"

ALLOW_EMPTY:${PN} = "0"

INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"

INSANE_SKIP:${PN} += "already-stripped ldflags"
