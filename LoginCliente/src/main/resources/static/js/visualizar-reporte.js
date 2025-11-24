var reporteData = null;

function eliminarReporte(idReporte) {
    Swal.fire({
        title: '¿Está seguro de que desea eliminar este reporte?',
        text: "Esta acción no se puede deshacer.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.value) {
            fetch('eliminar/'+idReporte, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            }).then(response => {
                if (response.ok) {
                    Swal.fire(
                        'Eliminado',
                        'El reporte ha sido eliminado exitosamente.',
                        'success'
                    ).then(() => {
                        // Recargar la página o actualizar la lista de reportes
                        window.location.reload();
                    });
                }
            });
        }
    });
}

function verReporte(idReporte) {
    document.getElementById("reporteModal").style.display = "block";
    cargarReporte(idReporte);
}

function cerrarModalReporte() {
    document.getElementById("reporteModal").style.display = "none";
}

async function cargarReporte(idReporte) {
    fetch(`/reportes/${idReporte}`)
        .then((response) => {
            if (!response.ok) {
                throw new Error("Error al cargar el reporte");
            }
            return response.text();
        })
        .then((data) => {
            if(data){
                let jsonData = JSON.parse(data);
                reporteData = jsonData;
                renderizarTodoReporte(jsonData)
                renderizarLibroDiario(jsonData)
                renderizarLibroMayor(jsonData)
                renderizarBalanceComprobacion(jsonData)
                renderizarEstadoResultados(jsonData)
                renderizarEstadoCapital(jsonData)
                renderizarBalanceGeneral(jsonData)
                renderizarFlujoEfectivo(jsonData)

                console.log(jsonData)
            }
        })
        .catch((error) => {
            console.error("Error fetching report data:", error);
            alert("No se pudo cargar el reporte. Por favor, intente nuevamente más tarde.");
        });
}

function cambiarTab(tabName, evt) {
    // Remover clase active de todos los botones
    document.querySelectorAll(".tab-button").forEach((btn) => {
        btn.classList.remove("active")
    })
    // Remover clase active de todos los tab panes
    document.querySelectorAll(".tab-pane").forEach((pane) => {
        pane.classList.remove("active")
    })

    // Determinar el botón que activó la acción: puede venir desde evt o buscarse por data-tab
    let btnToActivate = null
    if (evt && evt.target) {
        btnToActivate = evt.target.closest(".tab-button")
    }
    if (!btnToActivate) {
        btnToActivate = document.querySelector(`.tab-button[data-tab="${tabName}"]`)
    }
    if (btnToActivate) {
        btnToActivate.classList.add("active")
    }

    // Mostrar el tab correspondiente
    const tabId = "tab-" + tabName
    const tabElement = document.getElementById(tabId)
    if (tabElement) {
        tabElement.classList.add("active")
    }

    // Renderizar datos si ya existen
    if (reporteData && tabName === "all") {
        renderizarTodoReporte()
    }
}

function renderizarLibroDiario(data) {
    const tbody = document.getElementById("tbody-partida-doble")
    tbody.innerHTML = ""

    if (!data.libroDiario || data.libroDiario.length === 0) {
        tbody.innerHTML =
            '<tr><td colspan="6" style="text-align: center; color: #999;">No hay partidas en este período</td></tr>'
        return
    }

    data.libroDiario.forEach((partida) => {
        partida.movimientos.forEach((mov, index) => {
            const fila = document.createElement("tr")

            // Primera fila muestra el número de asiento, fecha y descripción
            if (index === 0) {
                fila.innerHTML = `
                    <td rowspan="${partida.movimientos.length}">${partida.idPartida}</td>
                    <td rowspan="${partida.movimientos.length}">${partida.fecha}</td>
                    <td><strong>${mov.nombreCuenta}</strong></td>
                    <td rowspan="${partida.movimientos.length}" style="font-style: italic; color: #555;">${partida.concepto || "---"}</td>
                    <td class="text-right">${mov.tipo === "D" ? "$" + mov.monto.toFixed(2) : ""}</td>
                    <td class="text-right">${mov.tipo === "H" ? "$" + mov.monto.toFixed(2) : ""}</td>
                `
            } else {
                // Filas siguientes solo muestran la cuenta y montos
                fila.innerHTML = `
                    <td><strong>${mov.nombreCuenta}</strong></td>
                    <td class="text-right">${mov.tipo === "D" ? "$" + mov.monto.toFixed(2) : ""}</td>
                    <td class="text-right">${mov.tipo === "H" ? "$" + mov.monto.toFixed(2) : ""}</td>
                `
            }

            tbody.appendChild(fila)
        })
    })
}

function renderizarLibroMayor(data) {
    const container = document.getElementById("cuentas-t-container")
    container.innerHTML = ""

    if (!data.libroMayor || Object.keys(data.libroMayor).length === 0) {
        container.innerHTML = '<div class="empty-state">No hay cuentas con movimientos en este período</div>'
        return
    }

    Object.values(data.libroMayor).forEach((cuenta, index) => {
        const tieneMovimientos = cuenta.detalles && cuenta.detalles.length > 0

        let cuentaHTML = ""

        if (!tieneMovimientos) {
            // Sin movimientos: mostrar formato de tabla simple
            cuentaHTML = `
                <div class="account-card">
                    <div class="account-header">
                        <div class="account-title">
                            <h3>${cuenta.nombre}</h3>
                            <p>${cuenta.tipo} - ${cuenta.naturaleza === "D" ? "Deudora" : "Acreedora"}</p>
                        </div>
                    </div>
                    <div class="account-summary">
                        <div class="summary-item">
                            <div class="summary-label">Total Debe</div>
                            <div class="summary-value">$${cuenta.totalDebe.toFixed(2)}</div>
                        </div>
                        <div class="summary-item">
                            <div class="summary-label">Total Haber</div>
                            <div class="summary-value">$${cuenta.totalHaber.toFixed(2)}</div>
                        </div>
                        <div class="summary-item">
                            <div class="summary-label">Saldo</div>
                            <div class="summary-value">$${cuenta.saldo.toFixed(2)}</div>
                        </div>
                    </div>
                </div>
            `
        } else {
            // Con movimientos: formato clásico de cuenta T
            const movimientosDebe = cuenta.detalles.filter((d) => d.tipo === "D")
            const movimientosHaber = cuenta.detalles.filter((d) => d.tipo === "H")
            const maxFilas = Math.max(movimientosDebe.length, movimientosHaber.length)

            let filasHTML = ""
            let saldoDebe = 0
            let saldoHaber = 0

            // Generar filas de movimientos
            for (let i = 0; i < maxFilas; i++) {
                const debe = movimientosDebe[i]
                const haber = movimientosHaber[i]

                let debeHTML = ""
                let haberHTML = ""

                if (debe) {
                    saldoDebe += debe.monto
                    debeHTML = `
                        <div style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #eee;">
                            <span style="font-size: 11px; color: #666;">Asiento #${debe.idPartida}</span>
                            <span style="font-weight: 600;">$${debe.monto.toFixed(2)}</span>
                        </div>
                    `
                }

                if (haber) {
                    saldoHaber += haber.monto
                    haberHTML = `
                        <div style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #eee;">
                            <span style="font-size: 11px; color: #666;">Asiento #${haber.idPartida}</span>
                            <span style="font-weight: 600;">$${haber.monto.toFixed(2)}</span>
                        </div>
                    `
                }

                filasHTML += `
                    <tr>
                        <td style="vertical-align: top; padding: 8px;">${debeHTML}</td>
                        <td style="vertical-align: top; padding: 8px;">${haberHTML}</td>
                    </tr>
                `
            }

            // Calcular saldo final
            const saldoFinal = Math.abs(saldoDebe - saldoHaber)
            const ladoSaldo = saldoDebe > saldoHaber ? "debe" : "haber"

            cuentaHTML = `
                <div class="account-card" style="margin-bottom: 30px;">
                    <div class="account-header" >
                        <div class="account-title">
                            <h3 style="margin: 0; font-size: 18px;">${cuenta.nombre}</h3>
                            <p style="margin: 5px 0 0 0; font-size: 12px; opacity: 0.9;">${cuenta.tipo} - ${cuenta.naturaleza === "D" ? "Deudora" : "Acreedora"}</p>
                        </div>
                    </div>
                    
                    <div class="cuenta-t-clasica">
                        <table style="width: 100%; border-collapse: collapse;">
                            <thead>
                                <tr style="background: #ecf0f1;">
                                    <th style="width: 50%; padding: 12px; text-align: center;  font-weight: 700; color: #d32f2f;">DEBE</th>
                                    <th style="width: 50%; padding: 12px; text-align: center; font-weight: 700; color: #388e3c;">HABER</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${filasHTML}
                            </tbody>
                            <tfoot>
                                <tr style="background: #f8f9fa; font-weight: 700; ">
                                    <td style="padding: 12px; text-align: right; color: #d32f2f;">
                                        Total: $${saldoDebe.toFixed(2)}
                                    </td>
                                    <td style="padding: 12px; text-align: right; color: #388e3c;">
                                        Total: $${saldoHaber.toFixed(2)}
                                    </td>
                                </tr>
                                <tr style="background: #fff3cd; font-weight: 700;">
                                    <td colspan="2" style="padding: 12px; text-align: center; font-size: 16px; color: #856404;">
                                        Saldo Final: $${saldoFinal.toFixed(2)} (${ladoSaldo === "debe" ? "DEUDOR" : "ACREEDOR"})
                                    </td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>
            `
        }

        container.innerHTML += cuentaHTML
    })
}

function renderizarBalanceComprobacion(data) {
    const tbody = document.getElementById("tbody-balance-comprobacion")
    tbody.innerHTML = ""

    if (!data.balanceComprobacion || data.balanceComprobacion.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: #999;">No hay datos</td></tr>'
        return
    }

    let totalDebe = 0
    let totalHaber = 0

    data.balanceComprobacion.forEach((cuenta) => {
        const debe = Number.parseFloat(cuenta.debe) || 0
        const haber = Number.parseFloat(cuenta.haber) || 0

        totalDebe += debe
        totalHaber += haber

        const fila = document.createElement("tr")
        fila.innerHTML = `
                <td>${cuenta.nombre}</td>
                <td>${cuenta.tipo}</td>
                <td class="text-right">$${debe.toFixed(2)}</td>
                <td class="text-right">$${haber.toFixed(2)}</td>
            `
        tbody.appendChild(fila)
    })

    // Agregar fila de totales
    const filaTotal = document.createElement("tr")
    filaTotal.style.fontWeight = "bold"
    filaTotal.style.backgroundColor = "#f0f0f0"
    filaTotal.innerHTML = `
            <td colspan="2">TOTALES</td>
            <td class="text-right">$${totalDebe.toFixed(2)}</td>
            <td class="text-right">$${totalHaber.toFixed(2)}</td>
        `
    tbody.appendChild(filaTotal)
}

function renderizarEstadoResultados(data) {
    const container = document.getElementById("estado-resultados-container")
    const totalIngresos = Number.parseFloat(data.totalIngresos) || 0
    const totalGastos = Number.parseFloat(data.totalGastos) || 0
    const utilidadNeta = Number.parseFloat(data.utilidadNeta) || 0

    let ingresosRows = ""
    if (data.ingresos && data.ingresos.length > 0) {
        data.ingresos.forEach((ingreso) => {
            ingresosRows += `
        <tr>
            <td class="fin-indent-1">${ingreso.nombre}</td>
            <td class="fin-amount">$${Number.parseFloat(ingreso.saldo).toFixed(2)}</td>
            <td></td>
        </tr>`
        })
    } else {
        ingresosRows = '<tr><td class="fin-indent-1" style="color:#999;">(Sin ingresos)</td><td></td><td></td></tr>'
    }

    let gastosRows = ""
    if (data.gastos && data.gastos.length > 0) {
        data.gastos.forEach((gasto) => {
            gastosRows += `
        <tr>
            <td class="fin-indent-1">${gasto.nombre}</td>
            <td class="fin-amount">$${Number.parseFloat(gasto.saldo).toFixed(2)}</td>
            <td></td>
        </tr>`
        })
    } else {
        gastosRows = '<tr><td class="fin-indent-1" style="color:#999;">(Sin gastos)</td><td></td><td></td></tr>'
    }

    const html = `
        <div class="financial-paper">
            <div class="fin-header">
                
                <h3>Estado de Resultados</h3>
                <div class="period">Del ${data.fechaInicio} al ${data.fechaFin}</div>
            </div>

            <table class="fin-table">
                <tr>
                    <td class="fin-row-header">INGRESOS</td>
                    <td></td>
                    <td></td>
                </tr>
                ${ingresosRows}
                <tr>
                    <td class="fin-indent-1" style="font-weight:bold;">Total de Ingresos</td>
                    <td></td>
                    <td class="fin-amount fin-total-line">$${totalIngresos.toFixed(2)}</td>
                </tr>

                <tr>
                    <td class="fin-row-header">MENOS: GASTOS</td>
                    <td></td>
                    <td></td>
                </tr>
                ${gastosRows}
                <tr>
                    <td class="fin-indent-1" style="font-weight:bold;">Total de Gastos</td>
                    <td></td>
                    <td class="fin-amount fin-total-line">$${totalGastos.toFixed(2)}</td>
                </tr>

                <tr style="height: 20px;"><td></td><td></td><td></td></tr>

                <tr>
                    <td class="fin-row-header">UTILIDAD (O PÉRDIDA) NETA</td>
                    <td></td>
                    <td class="fin-amount fin-grand-total">$${utilidadNeta.toFixed(2)}</td>
                </tr>
            </table>
        </div>
    `

    container.innerHTML = html
}

function renderizarEstadoCapital(data) {
    const container = document.getElementById("estado-capital-container")
    const capitalInicial = Number.parseFloat(data.totalCapitalInicial) || 0
    const utilidadNeta = Number.parseFloat(data.utilidadNeta) || 0
    const retiros = Number.parseFloat(data.totalRetiros) || 0
    const capitalFinal = Number.parseFloat(data.capitalFinal) || 0

    // Capital accounts detail
    let capitalRows = ""
    if (data.capitalAccounts && data.capitalAccounts.length > 0) {
        data.capitalAccounts.forEach((acc) => {
            capitalRows += `
            <tr>
                <td class="fin-indent-1">${acc.nombre}</td>
                <td class="fin-amount">$${Number.parseFloat(acc.saldo).toFixed(2)}</td>
                <td></td>
            </tr>
           `
        })
    }

    const html = `
        <div class="financial-paper">
            <div class="fin-header">
                <h3>Estado de Capital</h3>
                <div class="period">Del ${data.fechaInicio} al ${data.fechaFin}</div>
            </div>

            <table class="fin-table">
                <tr>
                    <td class="fin-row-header">CAPITAL CONTABLE</td>
                    <td></td>
                    <td></td>
                </tr>
                ${capitalRows}
                <tr>
                    <td class="fin-indent-1" style="font-weight:bold;">Capital Inicial</td>
                    <td></td>
                    <td class="fin-amount">$${capitalInicial.toFixed(2)}</td>
                </tr>

                <tr>
                    <td class="fin-indent-1">MÁS: Utilidad Neta del Periodo</td>
                    <td class="fin-amount">$${utilidadNeta.toFixed(2)}</td>
                    <td></td>
                </tr>
                <tr>
                    <td class="fin-indent-1"></td>
                    <td class="fin-amount fin-total-line">$${(capitalInicial + utilidadNeta).toFixed(2)}</td>
                    <td></td>
                </tr>

                <tr>
                    <td class="fin-indent-1">MENOS: Retiros</td>
                    <td class="fin-amount">$${retiros.toFixed(2)}</td>
                    <td></td>
                </tr>

                <tr style="height: 20px;"><td></td><td></td><td></td></tr>

                <tr>
                    <td class="fin-row-header">CAPITAL CONTABLE FINAL</td>
                    <td></td>
                    <td class="fin-amount fin-grand-total">$${capitalFinal.toFixed(2)}</td>
                </tr>
            </table>
        </div>
    `

    container.innerHTML = html
}

function renderizarBalanceGeneral(data) {
    const container = document.getElementById("balance-general-container")

    let totalActivos = 0
    let totalPasivos = 0
    // Use the calculated capital final from the backend/state instead of summing accounts again
    const totalCapital = Number.parseFloat(data.capitalFinal) || 0

    if (data.activos) {
        data.activos.forEach((a) => {
            totalActivos += Number.parseFloat(a.saldo) || 0
        })
    }

    if (data.pasivos) {
        data.pasivos.forEach((p) => {
            totalPasivos += Number.parseFloat(p.saldo) || 0
        })
    }

    // We do NOT sum capital accounts here manually as per requirements.
    // We use totalCapital which is the "Estado de Capital" result.

    const totalPasivoMasCapital = totalPasivos + totalCapital

    // Determinar si el balance está cuadrado (tolerancia pequeña)
    const isBalanced = Math.abs(totalActivos - totalPasivoMasCapital) < 0.005
    const difference = Math.abs(totalActivos - totalPasivoMasCapital)

    const html = `
        <div class="row">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0">Activos</h5>
                    </div>
                    <div class="card-body p-0">
                        <table class="table table-striped mb-0">
                            <thead>
                                <tr>
                                    <th>Cuenta</th>
                                    <th class="text-right">Saldo</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${
        data.activos && data.activos.length > 0
            ? data.activos
                .map(
                    (a) => `
                                    <tr>
                                        <td>${a.nombre}</td>
                                        <td class="text-right">$${Number.parseFloat(a.saldo).toFixed(2)}</td>
                                    </tr>
                                `,
                )
                .join("")
            : '<tr><td colspan="2" class="text-center text-muted">No hay activos</td></tr>'
    }
                            </tbody>
                            <tfoot>
                                <tr class="bg-light font-weight-bold ">
                                    <td>TOTAL ACTIVOS</td>
                                    <td class="text-right">$${totalActivos.toFixed(2)}</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="card mb-4">
                    <div class="card-header bg-danger text-white">
                        <h5 class="mb-0">Pasivos</h5>
                    </div>
                    <div class="card-body p-0">
                        <table class="table table-striped mb-0">
                            <thead>
                                <tr>
                                    <th>Cuenta</th>
                                    <th class="text-right">Saldo</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${
        data.pasivos && data.pasivos.length > 0
            ? data.pasivos
                .map(
                    (p) => `
                                    <tr>
                                        <td>${p.nombre}</td>
                                        <td class="text-right">$${Number.parseFloat(p.saldo).toFixed(2)}</td>
                                    </tr>
                                `,
                )
                .join("")
            : '<tr><td colspan="2" class="text-center text-muted">No hay pasivos</td></tr>'
    }
                            </tbody>
                            <tfoot>
                                <tr class="bg-light font-weight-bold">
                                    <td>TOTAL PASIVOS</td>
                                    <td class="text-right">$${totalPasivos.toFixed(2)}</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header bg-success text-white">
                        <h5 class="mb-0">Capital</h5>
                    </div>
                    <div class="card-body p-0">
                        <table class="table table-striped mb-0">
                            <thead>
                                <tr>
                                    <th>Cuenta</th>
                                    <th class="text-right">Saldo</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>Capital Contable del Propietario</td>
                                    <td class="text-right">$${totalCapital.toFixed(2)}</td>
                                </tr>
                            </tbody>
                            <tfoot>
                                <tr class="bg-light font-weight-bold">
                                    <td>TOTAL CAPITAL</td>
                                    <td class="text-right">$${totalCapital.toFixed(2)}</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>

                <!-- Resultado visual que permite comparar los totales y ver si está cuadrado -->
                <div class="resultado-module" style="margin-top: 2rem;">
                    <div class="resultado-card">
                        <div class="resultado-left">
                            <div class="resultado-title">Balance General: Activos = Pasivo + Capital</div>
                            <div class="resultado-values">
                                <div class="resultado-item">
                                    <div class="label">TOTAL ACTIVOS</div>
                                    <div class="value">$${totalActivos.toFixed(2)}</div>
                                </div>
                                <div class="resultado-item">
                                    <div class="label">TOTAL PASIVO + CAPITAL</div>
                                    <div class="value">$${totalPasivoMasCapital.toFixed(2)}</div>
                                </div>
                            </div>
                        </div>

                        <div class="resultado-right">
                            <div class="resultado-diff">Diferencia: $${difference.toFixed(2)}</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `

    container.innerHTML = html
}

function renderizarFlujoEfectivo(data) {
    const container = document.getElementById("flujo-efectivo-container")
    const fe = data.flujoEfectivo

    if (!fe) {
        container.innerHTML = '<div class="empty-state">No hay datos de flujo de efectivo</div>'
        return
    }

    const renderRows = (items) => {
        if (!items || items.length === 0)
            return '<tr><td colspan="2" style="color: #999; font-style: italic; padding-left: 20px;">Sin movimientos</td></tr>'
        return items
            .map(
                (item) => `
            <tr>
                <td class="fin-indent-1">${item.concepto || "Movimiento vario"}</td>
                <td class="fin-amount">$${Number.parseFloat(item.monto).toFixed(2)}</td>
            </tr>
        `,
            )
            .join("")
    }

    const html = `
        <div class="financial-paper">
            <div class="fin-header">
                <h3>Estado de Flujo de Efectivo</h3>
                <div class="period">Del ${data.fechaInicio} al ${data.fechaFin}</div>
            </div>

            <table class="fin-table">
                <!-- Operación -->
                <tr>
                    <td class="fin-row-header">ACTIVIDADES DE OPERACIÓN</td>
                    <td></td>
                </tr>
                ${renderRows(fe.actividadesOperacion)}
                <tr>
                    <td class="fin-indent-1" style="font-weight:bold;">Flujo neto de actividades de operación</td>
                    <td class="fin-amount fin-total-line">$${Number.parseFloat(fe.totalOperacion).toFixed(2)}</td>
                </tr>
                <tr style="height: 10px;"><td></td><td></td></tr>

                <!-- Inversión -->
                <tr>
                    <td class="fin-row-header">ACTIVIDADES DE INVERSIÓN</td>
                    <td></td>
                </tr>
                ${renderRows(fe.actividadesInversion)}
                <tr>
                    <td class="fin-indent-1" style="font-weight:bold;">Flujo neto de actividades de inversión</td>
                    <td class="fin-amount fin-total-line">$${Number.parseFloat(fe.totalInversion).toFixed(2)}</td>
                </tr>
                <tr style="height: 10px;"><td></td><td></td></tr>

                <!-- Financiamiento -->
                <tr>
                    <td class="fin-row-header">ACTIVIDADES DE FINANCIAMIENTO</td>
                    <td></td>
                </tr>
                ${renderRows(fe.actividadesFinanciamiento)}
                <tr>
                    <td class="fin-indent-1" style="font-weight:bold;">Flujo neto de actividades de financiamiento</td>
                    <td class="fin-amount fin-total-line">$${Number.parseFloat(fe.totalFinanciamiento).toFixed(2)}</td>
                </tr>
                
                <tr style="height: 20px;"><td></td><td></td></tr>

                <!-- Resumen -->
                <tr>
                    <td class="fin-row-header">INCREMENTO/DISMINUCIÓN NETO DE EFECTIVO</td>
                    <td class="fin-amount">$${Number.parseFloat(fe.flujoNetoTotal).toFixed(2)}</td>
                </tr>
                <tr>
                    <td class="fin-indent-1">MÁS: Efectivo y equivalentes al inicio del periodo</td>
                    <td class="fin-amount">$${Number.parseFloat(fe.saldoInicial).toFixed(2)}</td>
                </tr>
                <tr>
                    <td class="fin-row-header">EFECTIVO Y EQUIVALENTES AL FINAL DEL PERIODO</td>
                    <td class="fin-amount fin-grand-total">$${Number.parseFloat(fe.saldoFinal).toFixed(2)}</td>
                </tr>
            </table>
        </div>
    `
    container.innerHTML = html
}

function renderizarTodoReporte() {
    const tabAll = document.getElementById("tab-all")
    const html = `
            <h2>Resumen General del Reporte</h2>
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin: 20px 0;">
                <div style="background: #f9f9f9; padding: 20px; border-radius: 8px;">
                    <div style="font-size: 12px; color: #666; font-weight: 600;">Número de Asientos</div>
                    <div style="font-size: 24px; font-weight: 700; color: #333; margin-top: 10px;">${reporteData.libroDiario.length}</div>
                </div>
                <div style="background: #f9f9f9; padding: 20px; border-radius: 8px;">
                    <div style="font-size: 12px; color: #666; font-weight: 600;">Número de Cuentas</div>
                    <div style="font-size: 24px; font-weight: 700; color: #333; margin-top: 10px;">${Object.keys(reporteData.libroMayor || {}).length}</div>
                </div>
                <div style="background: #f9f9f9; padding: 20px; border-radius: 8px;">
                    <div style="font-size: 12px; color: #666; font-weight: 600;">Balance: Debe = Haber</div>
                    <div style="font-size: 24px; font-weight: 700; color: #27ae60; margin-top: 10px;">✓</div>
                </div>
            </div>
        `
    tabAll.innerHTML = html
}

function exportarPDF() {
    console.log("[v0] Iniciando exportación PDF directa")

    if (!reporteData) {
        Swal.fire({
            icon: "warning",
            title: "Sin datos",
            text: "Por favor genera un reporte primero",
        })
        return
    }

    // Show loading state
    Swal.fire({
        title: "Generando PDF...",
        html: "Por favor espera",
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading()
        },
    })

    // Use setTimeout to allow the UI to update before heavy processing
    setTimeout(() => {
        try {
            const { jsPDF } = window.jspdf
            const doc = new jsPDF("p", "mm", "a4")
            let yPosition = 20
            const pageHeight = doc.internal.pageSize.getHeight()
            const margin = 15

            // Título
            doc.setFontSize(16)
            doc.setFont(undefined, "bold")
            doc.text("Reporte Contable", margin, yPosition)
            yPosition += 10

            // Fecha de generación
            doc.setFontSize(10)
            doc.setFont(undefined, "normal")
            doc.text("Fecha de generación: " + new Date().toLocaleString("es-ES"), margin, yPosition)
            yPosition += 15

            // --- Partida Doble ---
            if (reporteData.libroDiario && reporteData.libroDiario.length > 0) {
                if (yPosition > pageHeight - 50) doc.addPage()

                doc.setFontSize(14)
                doc.setFont(undefined, "bold")
                doc.text("Libro Diario (Partida Doble)", margin, yPosition)
                yPosition += 10

                const dataDiario = []

                reporteData.libroDiario.forEach((partida) => {
                    // Agregar cada movimiento de la partida
                    partida.movimientos.forEach((mov, index) => {
                        if (index === 0) {
                            // Primera fila incluye número de asiento, fecha y concepto
                            dataDiario.push([
                                partida.idPartida,
                                partida.fecha,
                                mov.nombreCuenta,
                                partida.concepto || "---",
                                mov.tipo === "D" ? "$" + mov.monto.toFixed(2) : "",
                                mov.tipo === "H" ? "$" + mov.monto.toFixed(2) : "",
                            ])
                        } else {
                            // Filas siguientes solo muestran la cuenta y montos
                            dataDiario.push([
                                "",
                                "",
                                mov.nombreCuenta,
                                "",
                                mov.tipo === "D" ? "$" + mov.monto.toFixed(2) : "",
                                mov.tipo === "H" ? "$" + mov.monto.toFixed(2) : "",
                            ])
                        }
                    })

                    // Agregar línea separadora entre partidas
                    dataDiario.push(["", "", "", "", "", ""])
                })

                doc.autoTable({
                    head: [["# Asiento", "Fecha", "Cuenta", "Descripción", "Debe", "Haber"]],
                    body: dataDiario,
                    startY: yPosition,
                    margin: { left: margin, right: margin },
                    columnStyles: {
                        0: { halign: "center", cellWidth: 20 },
                        1: { halign: "center", cellWidth: 25 },
                        2: { halign: "left", cellWidth: 50 },
                        3: { halign: "left", cellWidth: 40 },
                        4: { halign: "right", cellWidth: 25 },
                        5: { halign: "right", cellWidth: 25 },
                    },
                    styles: {
                        fontSize: 9,
                        cellPadding: 3,
                    },
                    didDrawPage: (data) => {
                        yPosition = data.cursor.y + 5
                    },
                })
                yPosition = doc.lastAutoTable.finalY + 15
            }

            // --- Libro Mayor ---
            if (reporteData.libroMayor) {
                if (yPosition > pageHeight - 50) {
                    doc.addPage()
                    yPosition = 20
                }

                doc.setFontSize(14)
                doc.setFont(undefined, "bold")
                doc.text("Libro Mayor (Cuentas T)", margin, yPosition)
                yPosition += 10

                // Separar cuentas con y sin movimientos
                const cuentasConMovimiento = []
                const cuentasSinMovimiento = []

                Object.values(reporteData.libroMayor).forEach((cuenta) => {
                    if (cuenta.detalles && cuenta.detalles.length > 0) {
                        cuentasConMovimiento.push(cuenta)
                    } else {
                        cuentasSinMovimiento.push(cuenta)
                    }
                })

                // Mostrar cuentas CON movimiento con detalles
                cuentasConMovimiento.forEach((cuenta) => {
                    // Verificar espacio disponible
                    if (yPosition > pageHeight - 80) {
                        doc.addPage()
                        yPosition = 20
                    }

                    // Título de la cuenta
                    doc.setFontSize(12)
                    doc.setFont(undefined, "bold")
                    doc.text(
                        `${cuenta.nombre} - ${cuenta.tipo} (${cuenta.naturaleza === "D" ? "Deudora" : "Acreedora"})`,
                        margin,
                        yPosition,
                    )
                    yPosition += 8

                    // Preparar datos de movimientos
                    const movimientosDebe = cuenta.detalles.filter((d) => d.tipo === "D")
                    const movimientosHaber = cuenta.detalles.filter((d) => d.tipo === "H")
                    const maxFilas = Math.max(movimientosDebe.length, movimientosHaber.length)

                    const dataMovimientos = []

                    for (let i = 0; i < maxFilas; i++) {
                        const debe = movimientosDebe[i]
                        const haber = movimientosHaber[i]

                        dataMovimientos.push([
                            debe ? `Asiento #${debe.idPartida}` : "",
                            debe ? "$" + debe.monto.toFixed(2) : "",
                            haber ? `Asiento #${haber.idPartida}` : "",
                            haber ? "$" + haber.monto.toFixed(2) : "",
                        ])
                    }

                    // Agregar totales
                    dataMovimientos.push([
                        "TOTAL DEBE",
                        "$" + cuenta.totalDebe.toFixed(2),
                        "TOTAL HABER",
                        "$" + cuenta.totalHaber.toFixed(2),
                    ])

                    // Agregar saldo final
                    const saldoFinal = Math.abs(cuenta.totalDebe - cuenta.totalHaber)
                    const ladoSaldo = cuenta.totalDebe > cuenta.totalHaber ? "DEUDOR" : "ACREEDOR"
                    dataMovimientos.push([
                        {
                            content: `SALDO FINAL: $${saldoFinal.toFixed(2)} (${ladoSaldo})`,
                            colSpan: 4,
                            styles: { halign: "center", fontStyle: "bold", fillColor: [255, 243, 205] },
                        },
                    ])

                    doc.autoTable({
                        head: [["DEBE", "Monto", "HABER", "Monto"]],
                        body: dataMovimientos,
                        startY: yPosition,
                        margin: { left: margin, right: margin },
                        columnStyles: {
                            0: { halign: "left", cellWidth: 45 },
                            1: { halign: "right", cellWidth: 35 },
                            2: { halign: "left", cellWidth: 45 },
                            3: { halign: "right", cellWidth: 35 },
                        },
                        headStyles: {
                            fillColor: [236, 240, 241],
                            textColor: [0, 0, 0],
                            fontStyle: "bold",
                        },
                        styles: {
                            fontSize: 9,
                            cellPadding: 3,
                        },
                        didDrawPage: (data) => {
                            yPosition = data.cursor.y + 5
                        },
                    })

                    yPosition = doc.lastAutoTable.finalY + 12
                })

                // Mostrar cuentas SIN movimiento en tabla simple
                if (cuentasSinMovimiento.length > 0) {
                    if (yPosition > pageHeight - 50) {
                        doc.addPage()
                        yPosition = 20
                    }

                    doc.setFontSize(12)
                    doc.setFont(undefined, "bold")
                    doc.text("Cuentas sin Movimiento", margin, yPosition)
                    yPosition += 8

                    const dataSinMovimiento = cuentasSinMovimiento.map((cuenta) => [
                        cuenta.nombre,
                        cuenta.tipo,
                        cuenta.naturaleza === "D" ? "Deudora" : "Acreedora",
                        "$" + cuenta.totalDebe.toFixed(2),
                        "$" + cuenta.totalHaber.toFixed(2),
                        "$" + cuenta.saldo.toFixed(2),
                    ])

                    doc.autoTable({
                        head: [["Cuenta", "Tipo", "Naturaleza", "Total Debe", "Total Haber", "Saldo"]],
                        body: dataSinMovimiento,
                        startY: yPosition,
                        margin: { left: margin, right: margin },
                        columnStyles: {
                            3: { halign: "right" },
                            4: { halign: "right" },
                            5: { halign: "right" },
                        },
                        styles: {
                            fontSize: 9,
                            cellPadding: 3,
                        },
                        didDrawPage: (data) => {
                            yPosition = data.cursor.y + 5
                        },
                    })
                    yPosition = doc.lastAutoTable.finalY + 15
                }
            }

            // --- Balance de Comprobación ---
            if (reporteData.balanceComprobacion && reporteData.balanceComprobacion.length > 0) {
                if (yPosition > pageHeight - 50) {
                    doc.addPage()
                    yPosition = 20
                }

                doc.setFontSize(14)
                doc.setFont(undefined, "bold")
                doc.text("Balance de Comprobación", margin, yPosition)
                yPosition += 10

                const dataBC = reporteData.balanceComprobacion.map((cuenta) => [
                    cuenta.nombre,
                    cuenta.tipo,
                    "$" + (cuenta.debe ? Number.parseFloat(cuenta.debe).toFixed(2) : "0.00"),
                    "$" + (cuenta.haber ? Number.parseFloat(cuenta.haber).toFixed(2) : "0.00"),
                ])

                // Totales
                if (reporteData.totalesBalanceComprobacion) {
                    dataBC.push([
                        "TOTALES",
                        "",
                        "$" +
                        (reporteData.totalesBalanceComprobacion.debe
                            ? reporteData.totalesBalanceComprobacion.debe.toFixed(2)
                            : "0.00"),
                        "$" +
                        (reporteData.totalesBalanceComprobacion.haber
                            ? reporteData.totalesBalanceComprobacion.haber.toFixed(2)
                            : "0.00"),
                    ])
                }

                doc.autoTable({
                    head: [["Cuenta", "Tipo", "Debe", "Haber"]],
                    body: dataBC,
                    startY: yPosition,
                    margin: { left: margin, right: margin },
                    columnStyles: {
                        2: { halign: "right" },
                        3: { halign: "right" },
                    },
                    didDrawPage: (data) => {
                        yPosition = data.cursor.y + 5
                    },
                })
                yPosition = doc.lastAutoTable.finalY + 20 // Increased spacing
            }

            // --- Estado de Resultados ---
            if (reporteData.ingresos || reporteData.gastos) {
                if (yPosition > pageHeight - 60) {
                    // Increased page break threshold
                    doc.addPage()
                    yPosition = 20
                }

                doc.setFontSize(14)
                doc.setFont(undefined, "bold")
                doc.text("Estado de Resultados", margin, yPosition)
                yPosition += 15 // Increased spacing

                const dataER = []

                if (reporteData.ingresos && reporteData.ingresos.length > 0) {
                    dataER.push([{ content: "INGRESOS", colSpan: 2, styles: { fontStyle: "bold", fillColor: [240, 240, 240] } }])

                    let totalIngresosSection = 0
                    reporteData.ingresos.forEach((ingreso) => {
                        const val = Number.parseFloat(ingreso.saldo)
                        totalIngresosSection += val
                        dataER.push([ingreso.nombre, "$" + val.toFixed(2)])
                    })
                    dataER.push([
                        { content: "Total Ingresos", styles: { fontStyle: "bold" } },
                        { content: "$" + totalIngresosSection.toFixed(2), styles: { fontStyle: "bold", halign: "right" } },
                    ])
                }

                if (reporteData.gastos && reporteData.gastos.length > 0) {
                    dataER.push([{ content: "GASTOS", colSpan: 2, styles: { fontStyle: "bold", fillColor: [240, 240, 240] } }])

                    let totalGastosSection = 0
                    reporteData.gastos.forEach((gasto) => {
                        const val = Number.parseFloat(gasto.saldo)
                        totalGastosSection += val
                        dataER.push([gasto.nombre, "$" + val.toFixed(2)])
                    })
                    dataER.push([
                        { content: "Total Gastos", styles: { fontStyle: "bold" } },
                        { content: "$" + totalGastosSection.toFixed(2), styles: { fontStyle: "bold", halign: "right" } },
                    ])
                }

                const utilidadNeta = Number.parseFloat(reporteData.utilidadNeta) || 0
                const labelUtilidad = utilidadNeta >= 0 ? "UTILIDAD NETA" : "PÉRDIDA NETA"

                dataER.push([
                    { content: labelUtilidad, styles: { fontStyle: "bold", fontSize: 11 } },
                    { content: "$" + utilidadNeta.toFixed(2), styles: { fontStyle: "bold", fontSize: 11, halign: "right" } },
                ])

                doc.autoTable({
                    body: dataER,
                    startY: yPosition,
                    margin: { left: margin, right: margin },
                    columnStyles: {
                        1: { halign: "right", cellWidth: 50 },
                    },
                    theme: "grid", // Use grid theme for better readability
                    didDrawPage: (data) => {
                        yPosition = data.cursor.y + 5
                    },
                })
                yPosition = doc.lastAutoTable.finalY + 20 // Increased spacing
            }

            // --- Estado de Capital ---
            if (reporteData.capitalAccounts || reporteData.retiros) {
                if (yPosition > pageHeight - 60) {
                    // Increased page break threshold
                    doc.addPage()
                    yPosition = 20
                }

                doc.setFontSize(14)
                doc.setFont(undefined, "bold")
                doc.text("Estado de Capital", margin, yPosition)
                yPosition += 15 // Increased spacing

                const dataEC = []
                const capitalInicial = Number.parseFloat(reporteData.totalCapitalInicial) || 0
                const utilidadNeta = Number.parseFloat(reporteData.utilidadNeta) || 0
                const retiros = Number.parseFloat(reporteData.totalRetiros) || 0
                const capitalFinal = Number.parseFloat(reporteData.capitalFinal) || 0

                dataEC.push(["Capital Inicial", "$" + capitalInicial.toFixed(2)])
                dataEC.push(["Más: Utilidad Neta", "$" + utilidadNeta.toFixed(2)])
                if (retiros > 0) {
                    dataEC.push(["Menos: Retiros", "$" + retiros.toFixed(2)])
                }

                dataEC.push([
                    { content: "CAPITAL CONTABLE FINAL", styles: { fontStyle: "bold", fontSize: 11 } },
                    { content: "$" + capitalFinal.toFixed(2), styles: { fontStyle: "bold", fontSize: 11, halign: "right" } },
                ])

                doc.autoTable({
                    body: dataEC,
                    startY: yPosition,
                    margin: { left: margin, right: margin },
                    columnStyles: {
                        1: { halign: "right", cellWidth: 50 },
                    },
                    theme: "grid",
                    didDrawPage: (data) => {
                        yPosition = data.cursor.y + 5
                    },
                })
                yPosition = doc.lastAutoTable.finalY + 20 // Increased spacing
            }

            // --- Balance General ---
            if (reporteData.activos || reporteData.pasivos || reporteData.capital) {
                if (yPosition > pageHeight - 60) {
                    doc.addPage()
                    yPosition = 20
                }

                doc.setFontSize(14)
                doc.setFont(undefined, "bold")
                doc.text("Balance General", margin, yPosition)
                yPosition += 15 // Increased spacing

                const dataBalanceGeneral = []

                const addSection = (items, sectionName) => {
                    if (items && items.length > 0) {
                        dataBalanceGeneral.push([
                            { content: sectionName, colSpan: 2, styles: { fontStyle: "bold", fillColor: [220, 220, 220] } },
                        ])
                        let total = 0
                        items.forEach((item) => {
                            const val = Number.parseFloat(item.saldo) || 0
                            total += val
                            dataBalanceGeneral.push([item.nombre, "$" + val.toFixed(2)])
                        })
                        dataBalanceGeneral.push([
                            { content: "TOTAL " + sectionName, styles: { fontStyle: "bold" } },
                            { content: "$" + total.toFixed(2), styles: { fontStyle: "bold", halign: "right" } },
                        ])
                    }
                }

                addSection(reporteData.activos, "ACTIVOS")
                addSection(reporteData.pasivos, "PASIVOS")

                dataBalanceGeneral.push([
                    { content: "CAPITAL", colSpan: 2, styles: { fontStyle: "bold", fillColor: [220, 220, 220] } },
                ])
                const capitalFinal = Number.parseFloat(reporteData.capitalFinal) || 0
                dataBalanceGeneral.push(["Capital Contable del Propietario", "$" + capitalFinal.toFixed(2)])
                dataBalanceGeneral.push([
                    { content: "TOTAL CAPITAL", styles: { fontStyle: "bold" } },
                    { content: "$" + capitalFinal.toFixed(2), styles: { fontStyle: "bold", halign: "right" } },
                ])

                let totalActivos = 0
                if (reporteData.activos) reporteData.activos.forEach((a) => (totalActivos += Number.parseFloat(a.saldo)))

                let totalPasivos = 0
                if (reporteData.pasivos) reporteData.pasivos.forEach((p) => (totalPasivos += Number.parseFloat(p.saldo)))

                const totalPasivoCapital = totalPasivos + capitalFinal

                dataBalanceGeneral.push([{ content: "", colSpan: 2, styles: { fillColor: [255, 255, 255] } }]) // Spacer
                dataBalanceGeneral.push([
                    { content: "TOTAL ACTIVOS", styles: { fontStyle: "bold", fontSize: 10, fillColor: [230, 240, 255] } },
                    {
                        content: "$" + totalActivos.toFixed(2),
                        styles: { fontStyle: "bold", fontSize: 10, halign: "right", fillColor: [230, 240, 255] },
                    },
                ])
                dataBalanceGeneral.push([
                    {
                        content: "TOTAL PASIVO + CAPITAL",
                        styles: { fontStyle: "bold", fontSize: 10, fillColor: [230, 240, 255] },
                    },
                    {
                        content: "$" + totalPasivoCapital.toFixed(2),
                        styles: { fontStyle: "bold", fontSize: 10, halign: "right", fillColor: [230, 240, 255] },
                    },
                ])

                doc.autoTable({
                    body: dataBalanceGeneral,
                    startY: yPosition,
                    margin: { left: margin, right: margin },
                    columnStyles: {
                        1: { halign: "right" },
                    },
                    theme: "grid",
                })
            }

            // Guardar PDF
            const filename = "reporte_" + new Date().toISOString().split("T")[0] + ".pdf"
            doc.save(filename)

            Swal.close()
            Swal.fire({
                icon: "success",
                title: "Éxito",
                text: "PDF descargado correctamente",
            })
        } catch (error) {
            console.error("[v0] Error en exportarPDF:", error)
            Swal.close()
            Swal.fire({
                icon: "error",
                title: "Error al generar PDF",
                text: error.message || "Ocurrió un error inesperado",
            })
        }
    }, 500)
}