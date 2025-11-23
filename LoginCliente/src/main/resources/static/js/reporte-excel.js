function exportarExcel() {
    console.log("[v0] Iniciando exportación Excel separada")

    // Acceder a los datos globales (definidos en reporte.js)
    // Se asume que reporteData está disponible globalmente o en window
    const data = window.reporteData || window.reporteData

    if (!data) {
        window.Swal.fire({
            icon: "warning",
            title: "Sin datos",
            text: "Por favor genera un reporte primero",
        })
        return
    }

    if (typeof window.XLSX === "undefined") {
        window.Swal.fire({
            icon: "error",
            title: "Error de librería",
            text: "La librería XLSX no se ha cargado correctamente. Por favor verifica tu conexión a internet o recarga la página.",
        })
        return
    }

    window.Swal.fire({
        title: "Generando Excel...",
        html: "Por favor espera",
        allowOutsideClick: false,
        didOpen: () => {
            window.Swal.showLoading()
        },
    })

    // Usar setTimeout para permitir que la UI se actualice antes del procesamiento pesado
    setTimeout(() => {
        try {
            const workbook = window.XLSX.utils.book_new()

            // --- Libro Diario ---
            if (data.libroDiario && data.libroDiario.length > 0) {
                const dataDiario = []

                data.libroDiario.forEach((partida) => {
                    partida.movimientos.forEach((mov) => {
                        dataDiario.push({
                            "# Asiento": partida.idPartida,
                            Fecha: partida.fecha,
                            Descripción: partida.concepto || "---",
                            Cuenta: mov.nombreCuenta,
                            Debe: mov.tipo === "D" ? mov.monto : 0,
                            Haber: mov.tipo === "H" ? mov.monto : 0,
                        })
                    })
                })

                const ws1 = window.XLSX.utils.json_to_sheet(dataDiario)
                const wscols = [
                    { wch: 10 }, // # Asiento
                    { wch: 12 }, // Fecha
                    { wch: 40 }, // Descripción
                    { wch: 30 }, // Cuenta
                    { wch: 15 }, // Debe
                    { wch: 15 }, // Haber
                ]
                ws1["!cols"] = wscols
                window.XLSX.utils.book_append_sheet(workbook, ws1, "Libro Diario")
            }

            // --- Libro Mayor (Cuentas T Detalladas) ---
            if (data.libroMayor) {
                const rowsMayor = []
                const cuentasSinMovimiento = []

                Object.values(data.libroMayor).forEach((cuenta) => {
                    if (cuenta.detalles && cuenta.detalles.length > 0) {
                        rowsMayor.push(["Cuenta:", cuenta.nombre, "", ""])
                        rowsMayor.push(["Tipo:", cuenta.tipo, "Naturaleza:", cuenta.naturaleza === "D" ? "Deudora" : "Acreedora"])

                        rowsMayor.push(["DEBE", "", "HABER", ""])
                        rowsMayor.push(["Asiento", "Monto", "Asiento", "Monto"])

                        const movsDebe = cuenta.detalles.filter((d) => d.tipo === "D")
                        const movsHaber = cuenta.detalles.filter((d) => d.tipo === "H")
                        const maxRows = Math.max(movsDebe.length, movsHaber.length)

                        let totalDebe = 0
                        let totalHaber = 0

                        for (let i = 0; i < maxRows; i++) {
                            const d = movsDebe[i]
                            const h = movsHaber[i]

                            if (d) totalDebe += d.monto
                            if (h) totalHaber += h.monto

                            rowsMayor.push([d ? d.idPartida : "", d ? d.monto : "", h ? h.idPartida : "", h ? h.monto : ""])
                        }

                        rowsMayor.push(["Total Debe:", totalDebe, "Total Haber:", totalHaber])
                        rowsMayor.push(["Saldo Final:", cuenta.saldo, "", ""])
                        rowsMayor.push([]) // Empty row separator
                        rowsMayor.push([]) // Empty row separator
                    } else {
                        cuentasSinMovimiento.push({
                            Cuenta: cuenta.nombre,
                            Tipo: cuenta.tipo,
                            Naturaleza: cuenta.naturaleza === "D" ? "Deudora" : "Acreedora",
                            "Total Debe": cuenta.totalDebe,
                            "Total Haber": cuenta.totalHaber,
                            Saldo: cuenta.saldo,
                        })
                    }
                })

                if (rowsMayor.length > 0) {
                    const wsMayor = window.XLSX.utils.aoa_to_sheet(rowsMayor)
                    wsMayor["!cols"] = [{ wch: 15 }, { wch: 15 }, { wch: 15 }, { wch: 15 }]
                    window.XLSX.utils.book_append_sheet(workbook, wsMayor, "Libro Mayor (Detalle)")
                }

                if (cuentasSinMovimiento.length > 0) {
                    const wsSinMov = window.XLSX.utils.json_to_sheet(cuentasSinMovimiento)
                    wsSinMov["!cols"] = [{ wch: 30 }, { wch: 15 }, { wch: 15 }, { wch: 15 }, { wch: 15 }, { wch: 15 }]
                    window.XLSX.utils.book_append_sheet(workbook, wsSinMov, "Mayor (Sin Movimiento)")
                }
            }

            // --- Balance Comprobación ---
            if (data.balanceComprobacion && data.balanceComprobacion.length > 0) {
                const dataBC = data.balanceComprobacion.map((c) => ({
                    Cuenta: c.nombre,
                    Tipo: c.tipo,
                    Debe: Number.parseFloat(c.debe) || 0,
                    Haber: Number.parseFloat(c.haber) || 0,
                }))

                if (data.totalesBalanceComprobacion) {
                    dataBC.push({
                        Cuenta: "TOTALES",
                        Tipo: "",
                        Debe: data.totalesBalanceComprobacion.debe,
                        Haber: data.totalesBalanceComprobacion.haber,
                    })
                }

                const ws3 = window.XLSX.utils.json_to_sheet(dataBC)
                window.XLSX.utils.book_append_sheet(workbook, ws3, "Balance Comprobación")
            }

            // --- Estado de Resultados ---
            const dataER = []
            const utilidadNeta = Number.parseFloat(data.utilidadNeta) || 0

            dataER.push({ Concepto: "INGRESOS", Monto: "" })
            if (data.ingresos) {
                data.ingresos.forEach((i) => {
                    dataER.push({ Concepto: i.nombre, Monto: Number.parseFloat(i.saldo) || 0 })
                })
            }
            dataER.push({ Concepto: "Total Ingresos", Monto: Number.parseFloat(data.totalIngresos) || 0 })
            dataER.push({ Concepto: "", Monto: "" }) // Spacer

            dataER.push({ Concepto: "GASTOS", Monto: "" })
            if (data.gastos) {
                data.gastos.forEach((g) => {
                    dataER.push({ Concepto: g.nombre, Monto: Number.parseFloat(g.saldo) || 0 })
                })
            }
            dataER.push({ Concepto: "Total Gastos", Monto: Number.parseFloat(data.totalGastos) || 0 })
            dataER.push({ Concepto: "", Monto: "" }) // Spacer

            dataER.push({ Concepto: "UTILIDAD/PÉRDIDA NETA", Monto: utilidadNeta })

            if (dataER.length > 0) {
                const wsER = window.XLSX.utils.json_to_sheet(dataER)
                wsER["!cols"] = [{ wch: 40 }, { wch: 15 }]
                window.XLSX.utils.book_append_sheet(workbook, wsER, "Estado de Resultados")
            }

            // --- Estado de Capital ---
            const dataEC = []
            const capitalInicial = Number.parseFloat(data.totalCapitalInicial) || 0
            const retiros = Number.parseFloat(data.totalRetiros) || 0
            const capitalFinal = Number.parseFloat(data.capitalFinal) || 0

            dataEC.push({ Concepto: "Capital Inicial", Monto: capitalInicial })
            dataEC.push({ Concepto: "Más: Utilidad Neta", Monto: utilidadNeta })
            dataEC.push({ Concepto: "Menos: Retiros", Monto: retiros })
            dataEC.push({ Concepto: "CAPITAL CONTABLE FINAL", Monto: capitalFinal })

            if (dataEC.length > 0) {
                const wsEC = window.XLSX.utils.json_to_sheet(dataEC)
                wsEC["!cols"] = [{ wch: 40 }, { wch: 15 }]
                window.XLSX.utils.book_append_sheet(workbook, wsEC, "Estado de Capital")
            }

            // --- Balance General ---
            const dataBalanceGeneral = []
            const addRowsExcel = (items, sectionName) => {
                if (items && items.length > 0) {
                    dataBalanceGeneral.push({ Sección: sectionName, Cuenta: "", Saldo: "" }) // Header
                    let total = 0
                    items.forEach((item) => {
                        const val = Number.parseFloat(item.saldo) || 0
                        total += val
                        dataBalanceGeneral.push({
                            Sección: "",
                            Cuenta: item.nombre,
                            Saldo: val,
                        })
                    })
                    dataBalanceGeneral.push({
                        Sección: "",
                        Cuenta: "TOTAL " + sectionName,
                        Saldo: total,
                    })
                    dataBalanceGeneral.push({ Sección: "", Cuenta: "", Saldo: "" }) // Spacer
                }
            }

            addRowsExcel(data.activos, "ACTIVOS")
            addRowsExcel(data.pasivos, "PASIVOS")

            dataBalanceGeneral.push({ Sección: "CAPITAL", Cuenta: "", Saldo: "" })
            dataBalanceGeneral.push({
                Sección: "",
                Cuenta: "Capital Contable del Propietario",
                Saldo: capitalFinal,
            })
            dataBalanceGeneral.push({
                Sección: "",
                Cuenta: "TOTAL CAPITAL",
                Saldo: capitalFinal,
            })

            if (dataBalanceGeneral.length > 0) {
                const ws4 = window.XLSX.utils.json_to_sheet(dataBalanceGeneral)
                ws4["!cols"] = [{ wch: 15 }, { wch: 40 }, { wch: 15 }]
                window.XLSX.utils.book_append_sheet(workbook, ws4, "Balance General")
            }

            const filename = "reporte_contable_" + new Date().toISOString().split("T")[0] + ".xlsx"
            window.XLSX.writeFile(workbook, filename)

            window.Swal.close()
            window.Swal.fire({
                icon: "success",
                title: "Éxito",
                text: "Excel descargado correctamente",
            })
        } catch (error) {
            console.error("[v0] Error en exportarExcel:", error)
            window.Swal.close()
            window.Swal.fire({
                icon: "error",
                title: "Error al generar Excel",
                text: error.message || "Ocurrió un error inesperado",
            })
        }
    }, 500)
}
