# Copyright (C) 2017 - 2023 Armin Kuster  <akuster808@gmail.com>
# Released under the MIT license (see COPYING.MIT for the terms)

SUMARRY = "SCAP content for various platforms, upstream version"
HOME_URL = "https://www.open-scap.org/security-policies/scap-security-guide/"
LIC_FILES_CHKSUM = "file://LICENSE;md5=9bfa86579213cb4c6adaffface6b2820"
LICENSE = "BSD-3-Clause"

SRCREV = "dad85502ce8da722a6afc391346c41cee61e90a9"
SRC_URI = "git://github.com/ComplianceAsCode/content.git;branch=master;protocol=https"


DEPENDS = "openscap-native python3-pyyaml-native python3-jinja2-native libxml2-native expat-native coreutils-native"

S = "${WORKDIR}/git"

inherit cmake pkgconfig python3native python3targetconfig

STAGING_OSCAP_BUILDDIR = "${TMPDIR}/work-shared/openscap/oscap-build-artifacts"
export OSCAP_CPE_PATH="${STAGING_OSCAP_BUILDDIR}${datadir_native}/openscap/cpe"
export OSCAP_SCHEMA_PATH="${STAGING_OSCAP_BUILDDIR}${datadir_native}/openscap/schemas"
export OSCAP_XSLT_PATH="${STAGING_OSCAP_BUILDDIR}${datadir_native}/openscap/xsl"

OECMAKE_GENERATOR = "Unix Makefiles"

EXTRA_OECMAKE += "-DENABLE_PYTHON_COVERAGE=OFF -DSSG_PRODUCT_DEFAULT=OFF -DSSG_PRODUCT_EXAMPLE=ON"

B = "${S}/build"

do_configure[depends] += "openscap-native:do_install"

do_configure:prepend () {
    sed -i -e 's:NAMES\ sed:NAMES\ ${HOSTTOOLS_DIR}/sed:g' ${S}/CMakeLists.txt
    sed -i -e 's:NAMES\ grep:NAMES\ ${HOSTTOOLS_DIR}/grep:g' ${S}/CMakeLists.txt
}

FILES:${PN} += "${datadir}/xml"

RDEPENDS:${PN} = "openscap"
