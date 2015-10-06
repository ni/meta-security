SUMMARY = "ClamAV anti-virus utility for Unix - command-line interface"
DESCRIPTION = "ClamAV is an open source antivirus engine for detecting trojans, viruses, malware & other malicious threats."
HOMEPAGE = "http://www.clamav.net/index.html"
SECTION = "security"
LICENSE = "LGPL-2.1"
DEPENDS = "libtool db openssl zlib ncurses bzip2 libmspack"

LIC_FILES_CHKSUM = "file://COPYING.LGPL;beginline=2;endline=3;md5=4b89c05acc71195e9a06edfa2fa7d092"

SRC_URI = "https://launchpad.net/debian/+archive/primary/+files/clamav_0.98.5%2Bdfsg.orig.tar.xz;name=archive \
    file://0001-Change-paths-in-sample-conf-file-to-match-Debian.patch \
    file://0002-Add-an-additional-n-after-the-number-in-the-pidfile.patch \
    file://0003-unit_tests-increment-test-timeout-from-40secs-to-5mi.patch \
    file://0004-Fix-compiling-on-Hurd.patch \
    file://0005-Workaround-a-bug-in-libc-on-Hurd.patch \
    file://0006-remove-unnecessary-harmful-flags-from-libclamav.pc.patch \
    file://0008-Add-upstream-systemd-support-for-clamav-daemon-and-c.patch \
    file://0009-fix-ssize_t-size_t-off_t-printf-modifier.patch \
    file://0010-hardcode-LLVM-linker-flag-because-llvm-config-return.patch \
    file://0014-remove-AC_CONFIG_SRCDIR-llvm-configure-from-libclama.patch \
    file://0015-bb-10731-Allow-to-specificy-a-group-for-the-socket-o.patch \
    file://0016-clamav-milter-add-additinal-SMFIF_-flags-before-invo.patch \
    file://0017-Bump-.so-version-number.patch \
    file://0018-llvm-don-t-use-system-libs.patch \
    file://clamav-0001-clamdscan.patch \
    file://clamd.conf \
    file://freshclam.conf \
    file://volatiles.03_clamav \
"

SRC_URI[archive.md5sum] = "34d5e8698e57ce45c4a8c3c2cb211cf3"
SRC_URI[archive.sha256sum] = "0e353f646a0add17ca42e75ccfc7edf4f8b7c1acc972a86c317543f6b365db2d"

inherit autotools-brokensep pkgconfig useradd systemd

UID = "clamav"
GID = "clamav"

S = "${WORKDIR}/${BPN}-${PV}+dfsg"

PACKAGECONFIG ??= ""
PACKAGECONFIG += " ${@bb.utils.contains("DISTRO_FEATURES", "ipv6", "ipv6", "", d)}"
PACKAGECONFIG += "${@base_contains('DISTRO_FEATURES', 'systemd', 'systemd', '', d)}"
PACKAGECONFIG[xml] = "--with-xml=${STAGING_LIBDIR}/.., --with-xml=no, libxml2,"
PACKAGECONFIG[json] = "--with-libjson=${STAGING_LIBDIR}, --without-libjson, json,"
PACKAGECONFIG[curl] = "--with-libcurl=${STAGING_LIBDIR}, --without-libcurl, curl,"
PACKAGECONFIG[ipv6] = "--enable-ipv6, --disable-ipv6"
PACKAGECONFI[systemd] = "--with-systemdsystemunitdir=${systemd_unitdir}/system/', '--without-systemdsystemunitdir', "

EXTRA_OECONF += " --with-user=${UID}  --with-group=${GID} \
            --without-libcheck-prefix --disable-unrar \
            --disable-llvm \
            --with-openssl=${STAGING_LIBDIR}/.. \
            --with-zlib=${STAGING_LIBDIR}/.. \
            --with-libbz2-prefix=${STAGING_DIR}${prefix} \
            --with-libncurses-prefix=${STAGING_LIBDIR}/.. \
"

do_install_append () {
    install -d ${D}/${sysconfdir}
    install -d ${D}/${localstatedir}/lib/clamav
    install -d ${D}${sysconfdir}/clamav ${D}${sysconfdir}/default/volatiles

    install -m 644 ${WORKDIR}/clamd.conf ${D}/${sysconfdir}
    install -m 644 ${WORKDIR}/freshclam.conf ${D}/${sysconfdir}
    install -m 0644 ${WORKDIR}/volatiles.03_clamav  ${D}${sysconfdir}/default/volatiles/volatiles.03_clamav
}

pkg_postinst_${PN} () {
    if [ -z "$D" ] && [ -e /etc/init.d/populate-volatile.sh ] ; then
        ${sysconfdir}/init.d/populate-volatile.sh update
    fi
    chown ${UID}:${GID} ${localstatedir}/lib/clamav
}


PACKAGES = "${PN} ${PN}-dev ${PN}-dbg ${PN}-daemon ${PN}-doc \
            ${PN}-clamdscan ${PN}-freshclam ${PN}-libclamav6 ${PN}-staticdev"

FILES_${PN} = "${bindir}/clambc ${bindir}/clamscan ${bibdir}/clamsubmit \
                ${bindir}/sigtool ${mandir}/man1/clambc* ${mandir}/man1/clamscan* \
                ${mandir}/man1/sigtool* ${mandir}/man1/clambsubmit*  \
                ${docdir}/clamav/* "

FILES_${PN}-clamdscan = " ${bindir}/clamdscan \
                        ${docdir}/clamdscan/* \
                        ${mandir}/man1/clamdscan* \
                        "

FILES_${PN}-daemon = "${bindir}/clamconf ${bindir}/clamdtop ${sbindir}/clamd \
                        ${mandir}/man1/clamconf* ${mandir}/man1/clamdtop* \
                        ${mandir}/man5/clamd*  ${mandir}/man8/clamd* \
                        ${sysconfdir}/clamd.conf* \
                        ${systemd_unitdir}/system/clamav-daemon/* \
                        ${docdir}/clamav-daemon/*  ${sysconfdir}/clamav-daemon \
                        ${sysconfdir}/logcheck/ignore.d.server/clamav-daemon "

FILES_${PN}-freshclam = "${bindir}/freshclam \
                        ${sysconfdir}/freshclam.conf*  \
                        ${sysconfdir}/clamav ${sysconfdir}/default/volatiles \
                        ${localstatedir}/lib/clamav \
                        ${docdir}/${PN}-freshclam ${mandir}/man1/freshclam.* \
                        ${mandir}/man5/freshclam.conf.*"

FILES_${PN}-dev = " ${bindir}/clamav-config ${libdir}/*.la \
                    ${libdir}/pkgconfig/*.pc \
                    ${mandir}/man1/clamav-config.* \
                    ${includedir}/*.h ${docdir}/libclamav* "

FILES_${PN}-staticdev = "${libdir}/*.a"

FILES_${PN}-libclamav6 = "${libdir}/libclamav.so* ${libdir}/libmspack.so*\
                          ${docdir}/libclamav6/* "

FILES_${PN}-doc = "${mandir}/man/* \
                    ${datadir}/man/* \
                   ${docdir}/* "

INSANE_SKIP_${PN}-libclamav6 = "dev-so"

USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM_${PN} = "--system ${UID}"
USERADD_PARAM_${PN} = "--system -g ${GID} --home-dir  \
    ${localstatedir}/spool/${BPN} \
    --no-create-home  --shell /bin/false ${BPN}"

RPROVIDES_${PN} += "${PN}-systemd"
RREPLACES_${PN} += "${PN}-systemd"
RCONFLICTS_${PN} += "${PN}-systemd"
SYSTEMD_SERVICE_${PN} = "${BPN}.service"

RDEPENDS_${PN} += "openssl ncurses-libncurses libbz2 ncurses-libtinfo clamav-freshclam clamav-libclamav6"
