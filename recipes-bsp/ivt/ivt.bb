DESCRIPTION = "IVT"
LICENSE = "BSD-3-Clause"

inherit deploy

S = "${WORKDIR}"

IVT_DCD_PTR = "00200000"
IVT_HSE_PTR = "00400000"
IVT_ATF_PTR = "00001200"
IVT_M7_PTR  = "00500000"
IVT_BOOT_CFG_M7 ="00000000"
IVT_BOOT_CFG_A53 ="00000001"

do_compile() {
    for plat in ${UBOOT_CONFIG}; do
        target_name="${PN}_${PV}-${plat}.bin"

        params="-o ${target_name}"

        if ${@bb.utils.contains("MACHINE_FEATURES", "m7_image", "true", "false", d)}; then
            # M7 app pointer
            params="${params} -a ${IVT_M7_PTR}"
            # Boot configuration word
            params="${params} -b ${IVT_BOOT_CFG_M7}"
        else
            # ATF app pointer
            params="${params} -a ${IVT_ATF_PTR}"
            # Boot configuration word
            params="${params} -b ${IVT_BOOT_CFG_A53}"
        fi

        if ${@bb.utils.contains("MACHINE_FEATURES", "hse_image", "true", "false", d)}; then
            # HSE pointer
            params="${params} -h ${IVT_HSE_PTR}"
        fi

        if ${@bb.utils.contains("MACHINE_FEATURES", "dcd_image", "true", "false", d)}; then
            # DCD pointer
            params="${params} -d ${IVT_DCD_PTR}"
        fi

        "${LAYER_PATH_aptiv-s32g-layer}"/scripts/create_ivt.sh ${params}

        md5sum="$(md5sum ${target_name} | awk '{print $1}')"
        echo "${md5sum}  ${nonarch_libdir}/firmware/${PN}-${plat}.bin" > "${target_name}.md5"
    done
}

do_install() {
    install -d "${D}${nonarch_libdir}/firmware"

    for plat in ${UBOOT_CONFIG}; do
        target_name="${PN}_${PV}-${plat}.bin"

        install -D -m 0644 "${S}/${target_name}" "${D}/${nonarch_libdir}/firmware/"
        install -D -m 0644 "${S}/${target_name}.md5" "${D}/${nonarch_libdir}/firmware/"
        ln -sf "${target_name}" "${D}/${nonarch_libdir}/firmware/${PN}-${plat}.bin"
        ln -sf "${target_name}.md5" "${D}/${nonarch_libdir}/firmware/${PN}-${plat}.bin.md5"
    done
}

do_deploy() {
    for plat in ${UBOOT_CONFIG}; do
        target_name="${PN}_${PV}-${plat}.bin"

        install -D -m 0644 "${B}/${target_name}" "${DEPLOYDIR}/"
        install -D -m 0644 "${B}/${target_name}.md5" "${DEPLOYDIR}/"
        ln -sf "${target_name}" "${DEPLOYDIR}/${PN}-${plat}.bin"
        ln -sf "${target_name}.md5" "${DEPLOYDIR}/${PN}-${plat}.bin.md5"
    done
}

do_patch() {
:
}

do_configure() {
:
}

FILES:${PN}:append = " \
    ${nonarch_libdir}/ \
    ${nonarch_libdir}/firmware/ \
    ${nonarch_libdir}/firmware/${PN}*.bin* \
    ${DEPLOY_DIR_IMAGE}/ \
    ${DEPLOY_DIR_IMAGE}/${PN}*.bin* \
"

addtask deploy after do_compile before do_install
EXPORT_FUNCTIONS do_deploy

EXCLUDE_FROM_WORLD = "1"

COMPATIBLE_MACHINE = "aptiv-cvc"
