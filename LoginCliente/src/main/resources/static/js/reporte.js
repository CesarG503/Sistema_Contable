var reporteData = null;
let tipoExportacionActual = null;

function cambiarTab(tabName, evt) {
    // Remover clase active de todos los botones
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });
    // Remover clase active de todos los tab panes
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.remove('active');
    });

    // Determinar el botón que activó la acción: puede venir desde evt o buscarse por data-tab
    let btnToActivate = null;
    if (evt && evt.target) {
        btnToActivate = evt.target.closest('.tab-button');
    }
    if (!btnToActivate) {
        btnToActivate = document.querySelector(`.tab-button[data-tab="${tabName}"]`);
    }
    if (btnToActivate) {
        btnToActivate.classList.add('active');
    }

    // Mostrar el tab correspondiente
    const tabId = 'tab-' + tabName;
    const tabElement = document.getElementById(tabId);
    if (tabElement) {
        tabElement.classList.add('active');
    }

    // Renderizar datos si ya existen
    if (reporteData && tabName === 'all') {
        renderizarTodoReporte();
    }
}

function generarReporte() {
    const fechaInicio = document.getElementById('fechaInicio').value;
    const fechaFin = document.getElementById('fechaFin').value;

    console.log("[v0] Generando reporte desde:", fechaInicio, "hasta:", fechaFin);

    if (!fechaInicio || !fechaFin) {
        Swal.fire({
            icon: 'warning',
            title: 'Campos incompletos',
            text: 'Por favor completa ambas fechas'
        });
        return;
    }

    if (new Date(fechaInicio) > new Date(fechaFin)) {
        Swal.fire({
            icon: 'error',
            title: 'Fecha inválida',
            text: 'La fecha inicial no puede ser mayor a la fecha final'
        });
        return;
    }

    document.getElementById('loading').classList.add('show');

    fetch('/reportes/obtener-datos', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            fechaInicio: fechaInicio,
            fechaFin: fechaFin
        })
    })
        .then(response => {
            console.log("[v0] Response status:", response.status);
            return response.json();
        })
        .then(data => {
            console.log("[v0] Datos recibidos:", data);
            document.getElementById('loading').classList.remove('show');

            if (data.error) {
                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: data.error
                });
                console.error("[v0] Error en datos:", data.error);
                return;
            }

            reporteData = data;
            console.log("[v0] Reporte data asignado:", reporteData);

            renderizarLibroDiario(data);
            renderizarLibroMayor(data);
            renderizarBalanceComprobacion(data);
            renderizarBalanceGeneral(data);

            Swal.fire({
                icon: 'success',
                title: 'Éxito',
                text: 'Reporte generado correctamente'
            });

            cambiarTab('partida-doble');
        })
        .catch(error => {
            document.getElementById('loading').classList.remove('show');
            console.error('[v0] Error de fetch:', error);
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'Error al generar el reporte: ' + error.message
            });
        });
}

function renderizarLibroDiario(data) {
    const tbody = document.getElementById('tbody-partida-doble');
    tbody.innerHTML = '';

    if (!data.libroDiario || data.libroDiario.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: #999;">No hay partidas en este período</td></tr>';
        return;
    }

    data.libroDiario.forEach(partida => {
        partida.movimientos.forEach((mov, index) => {
            const fila = document.createElement('tr');

            // Primera fila muestra el número de asiento, fecha y descripción
            if (index === 0) {
                fila.innerHTML = `
                    <td rowspan="${partida.movimientos.length}">${partida.idPartida}</td>
                    <td rowspan="${partida.movimientos.length}">${partida.fecha}</td>
                    <td><strong>${mov.nombreCuenta}</strong></td>
                    <td rowspan="${partida.movimientos.length}" style="font-style: italic; color: #555;">${partida.concepto || '---'}</td>
                    <td class="text-right">${mov.tipo === 'D' ? '$' + mov.monto.toFixed(2) : ''}</td>
                    <td class="text-right">${mov.tipo === 'H' ? '$' + mov.monto.toFixed(2) : ''}</td>
                `;
            } else {
                // Filas siguientes solo muestran la cuenta y montos
                fila.innerHTML = `
                    <td><strong>${mov.nombreCuenta}</strong></td>
                    <td class="text-right">${mov.tipo === 'D' ? '$' + mov.monto.toFixed(2) : ''}</td>
                    <td class="text-right">${mov.tipo === 'H' ? '$' + mov.monto.toFixed(2) : ''}</td>
                `;
            }

            tbody.appendChild(fila);
        });
    });
}

function renderizarLibroMayor(data) {
    const container = document.getElementById('cuentas-t-container');
    container.innerHTML = '';

    if (!data.libroMayor || Object.keys(data.libroMayor).length === 0) {
        container.innerHTML = '<div class="empty-state">No hay cuentas con movimientos en este período</div>';
        return;
    }

    Object.values(data.libroMayor).forEach((cuenta, index) => {
        const tieneMovimientos = cuenta.detalles && cuenta.detalles.length > 0;

        let cuentaHTML = '';

        if (!tieneMovimientos) {
            // Sin movimientos: mostrar formato de tabla simple
            cuentaHTML = `
                <div class="account-card">
                    <div class="account-header">
                        <div class="account-title">
                            <h3>${cuenta.nombre}</h3>
                            <p>${cuenta.tipo} - ${cuenta.naturaleza === 'D' ? 'Deudora' : 'Acreedora'}</p>
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
            `;
        } else {
            // Con movimientos: formato clásico de cuenta T
            const movimientosDebe = cuenta.detalles.filter(d => d.tipo === 'D');
            const movimientosHaber = cuenta.detalles.filter(d => d.tipo === 'H');
            const maxFilas = Math.max(movimientosDebe.length, movimientosHaber.length);

            let filasHTML = '';
            let saldoDebe = 0;
            let saldoHaber = 0;

            // Generar filas de movimientos
            for (let i = 0; i < maxFilas; i++) {
                const debe = movimientosDebe[i];
                const haber = movimientosHaber[i];

                let debeHTML = '';
                let haberHTML = '';

                if (debe) {
                    saldoDebe += debe.monto;
                    debeHTML = `
                        <div style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #eee;">
                            <span style="font-size: 11px; color: #666;">Asiento #${debe.idPartida}</span>
                            <span style="font-weight: 600;">$${debe.monto.toFixed(2)}</span>
                        </div>
                    `;
                }

                if (haber) {
                    saldoHaber += haber.monto;
                    haberHTML = `
                        <div style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #eee;">
                            <span style="font-size: 11px; color: #666;">Asiento #${haber.idPartida}</span>
                            <span style="font-weight: 600;">$${haber.monto.toFixed(2)}</span>
                        </div>
                    `;
                }

                filasHTML += `
                    <tr>
                        <td style="vertical-align: top; padding: 8px;">${debeHTML}</td>
                        <td style="vertical-align: top; padding: 8px;">${haberHTML}</td>
                    </tr>
                `;
            }

            // Calcular saldo final
            const saldoFinal = Math.abs(saldoDebe - saldoHaber);
            const ladoSaldo = saldoDebe > saldoHaber ? 'debe' : 'haber';

            cuentaHTML = `
                <div class="account-card" style="margin-bottom: 30px;">
                    <div class="account-header" >
                        <div class="account-title">
                            <h3 style="margin: 0; font-size: 18px;">${cuenta.nombre}</h3>
                            <p style="margin: 5px 0 0 0; font-size: 12px; opacity: 0.9;">${cuenta.tipo} - ${cuenta.naturaleza === 'D' ? 'Deudora' : 'Acreedora'}</p>
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
                                        Saldo Final: $${saldoFinal.toFixed(2)} (${ladoSaldo === 'debe' ? 'DEUDOR' : 'ACREEDOR'})
                                    </td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>
            `;
        }

        container.innerHTML += cuentaHTML;
    });
}

function renderizarBalanceComprobacion(data) {
    const tbody = document.getElementById('tbody-balance-comprobacion');
    tbody.innerHTML = '';

    if (!data.balanceComprobacion || data.balanceComprobacion.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: #999;">No hay datos</td></tr>';
        return;
    }

    let totalDebe = 0;
    let totalHaber = 0;

    data.balanceComprobacion.forEach(cuenta => {
        const debe = parseFloat(cuenta.debe) || 0;
        const haber = parseFloat(cuenta.haber) || 0;

        totalDebe += debe;
        totalHaber += haber;

        const fila = document.createElement('tr');
        fila.innerHTML = `
                <td>${cuenta.nombre}</td>
                <td>${cuenta.tipo}</td>
                <td class="text-right">$${debe.toFixed(2)}</td>
                <td class="text-right">$${haber.toFixed(2)}</td>
            `;
        tbody.appendChild(fila);
    });

    // Agregar fila de totales
    const filaTotal = document.createElement('tr');
    filaTotal.style.fontWeight = 'bold';
    filaTotal.style.backgroundColor = '#f0f0f0';
    filaTotal.innerHTML = `
            <td colspan="2">TOTALES</td>
            <td class="text-right">$${totalDebe.toFixed(2)}</td>
            <td class="text-right">$${totalHaber.toFixed(2)}</td>
        `;
    tbody.appendChild(filaTotal);
}

function renderizarBalanceGeneral(data) {
    const container = document.getElementById('balance-general-container');

    let totalActivos = 0;
    let totalPasivos = 0;
    let totalCapital = 0;

    if (data.activos) {
        data.activos.forEach(a => {
            totalActivos += parseFloat(a.saldo) || 0;
        });
    }

    if (data.pasivos) {
        data.pasivos.forEach(p => {
            totalPasivos += parseFloat(p.saldo) || 0;
        });
    }

    if (data.capital) {
        data.capital.forEach(c => {
            totalCapital += parseFloat(c.saldo) || 0;
        });
    }

    const html = `
            <div class="balance-grid">
                <div class="balance-column">
                    <h4>ACTIVOS</h4>
                    ${data.activos && data.activos.length > 0 ?
        data.activos.map(a => `<div style="margin-bottom: 8px; display: flex; justify-content: space-between;"><span>${a.nombre}</span><span>$${parseFloat(a.saldo).toFixed(2)}</span></div>`).join('')
        : '<p style="color: #999; font-size: 12px;">Sin datos</p>'
    }
                    <div class="balance-total">$${totalActivos.toFixed(2)}</div>
                </div>

                <div class="balance-column">
                    <h4>PASIVOS</h4>
                    ${data.pasivos && data.pasivos.length > 0 ?
        data.pasivos.map(p => `<div style="margin-bottom: 8px; display: flex; justify-content: space-between;"><span>${p.nombre}</span><span>$${parseFloat(p.saldo).toFixed(2)}</span></div>`).join('')
        : '<p style="color: #999; font-size: 12px;">Sin datos</p>'
    }
                    <div class="balance-total">$${totalPasivos.toFixed(2)}</div>
                </div>

                <div class="balance-column">
                    <h4>CAPITAL</h4>
                    ${data.capital && data.capital.length > 0 ?
        data.capital.map(c => `<div style="margin-bottom: 8px; display: flex; justify-content: space-between;"><span>${c.nombre}</span><span>$${parseFloat(c.saldo).toFixed(2)}</span></div>`).join('')
        : '<p style="color: #999; font-size: 12px;">Sin datos</p>'
    }
                    <div class="balance-total">$${totalCapital.toFixed(2)}</div>
                </div>
            </div>
        `;

    container.innerHTML = html;
}

function renderizarTodoReporte() {
    const tabAll = document.getElementById('tab-all');
    let html = `
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
        `;
    tabAll.innerHTML = html;
}

function exportarPDF() {
    console.log("[v0] Iniciando exportación PDF directa");

    if (!reporteData) {
        Swal.fire({
            icon: 'warning',
            title: 'Sin datos',
            text: 'Por favor genera un reporte primero'
        });
        return;
    }

    // Show loading state
    Swal.fire({
        title: 'Generando PDF...',
        html: 'Por favor espera',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    // Use setTimeout to allow the UI to update before heavy processing
    setTimeout(() => {
        try {
            const { jsPDF } = window.jspdf;
            const doc = new jsPDF('p', 'mm', 'a4');
            let yPosition = 20;
            const pageHeight = doc.internal.pageSize.getHeight();
            const margin = 15;

            // Título
            doc.setFontSize(16);
            doc.setFont(undefined, 'bold');
            doc.text('Reporte Contable', margin, yPosition);
            yPosition += 10;

            // Fecha de generación
            doc.setFontSize(10);
            doc.setFont(undefined, 'normal');
            doc.text('Fecha de generación: ' + new Date().toLocaleString('es-ES'), margin, yPosition);
            yPosition += 15;

            // --- Partida Doble ---
            if (reporteData.libroDiario && reporteData.libroDiario.length > 0) {
                if (yPosition > pageHeight - 50) doc.addPage();

                doc.setFontSize(14);
                doc.setFont(undefined, 'bold');
                doc.text('Libro Diario (Partida Doble)', margin, yPosition);
                yPosition += 10;

                const dataDiario = [];

                reporteData.libroDiario.forEach(partida => {
                    // Agregar cada movimiento de la partida
                    partida.movimientos.forEach((mov, index) => {
                        if (index === 0) {
                            // Primera fila incluye número de asiento, fecha y concepto
                            dataDiario.push([
                                partida.idPartida,
                                partida.fecha,
                                mov.nombreCuenta,
                                partida.concepto || '---',
                                mov.tipo === 'D' ? '$' + mov.monto.toFixed(2) : '',
                                mov.tipo === 'H' ? '$' + mov.monto.toFixed(2) : ''
                            ]);
                        } else {
                            // Filas siguientes solo muestran la cuenta y montos
                            dataDiario.push([
                                '',
                                '',
                                mov.nombreCuenta,
                                '',
                                mov.tipo === 'D' ? '$' + mov.monto.toFixed(2) : '',
                                mov.tipo === 'H' ? '$' + mov.monto.toFixed(2) : ''
                            ]);
                        }
                    });

                    // Agregar línea separadora entre partidas
                    dataDiario.push(['', '', '', '', '', '']);
                });

                doc.autoTable({
                    head: [['# Asiento', 'Fecha', 'Cuenta', 'Descripción', 'Debe', 'Haber']],
                    body: dataDiario,
                    startY: yPosition,
                    margin: { left: margin, right: margin },
                    columnStyles: {
                        0: { halign: 'center', cellWidth: 20 },
                        1: { halign: 'center', cellWidth: 25 },
                        2: { halign: 'left', cellWidth: 50 },
                        3: { halign: 'left', cellWidth: 40 },
                        4: { halign: 'right', cellWidth: 25 },
                        5: { halign: 'right', cellWidth: 25 }
                    },
                    styles: {
                        fontSize: 9,
                        cellPadding: 3
                    },
                    didDrawPage: function(data) {
                        yPosition = data.cursor.y + 5;
                    }
                });
                yPosition = doc.lastAutoTable.finalY + 15;
            }

            // --- Libro Mayor ---
            if (reporteData.libroMayor) {
                if (yPosition > pageHeight - 50) {
                    doc.addPage();
                    yPosition = 20;
                }

                doc.setFontSize(14);
                doc.setFont(undefined, 'bold');
                doc.text('Libro Mayor (Cuentas T)', margin, yPosition);
                yPosition += 10;

                // Separar cuentas con y sin movimientos
                const cuentasConMovimiento = [];
                const cuentasSinMovimiento = [];

                Object.values(reporteData.libroMayor).forEach(cuenta => {
                    if (cuenta.detalles && cuenta.detalles.length > 0) {
                        cuentasConMovimiento.push(cuenta);
                    } else {
                        cuentasSinMovimiento.push(cuenta);
                    }
                });

                // Mostrar cuentas CON movimiento con detalles
                cuentasConMovimiento.forEach(cuenta => {
                    // Verificar espacio disponible
                    if (yPosition > pageHeight - 80) {
                        doc.addPage();
                        yPosition = 20;
                    }

                    // Título de la cuenta
                    doc.setFontSize(12);
                    doc.setFont(undefined, 'bold');
                    doc.text(`${cuenta.nombre} - ${cuenta.tipo} (${cuenta.naturaleza === 'D' ? 'Deudora' : 'Acreedora'})`, margin, yPosition);
                    yPosition += 8;

                    // Preparar datos de movimientos
                    const movimientosDebe = cuenta.detalles.filter(d => d.tipo === 'D');
                    const movimientosHaber = cuenta.detalles.filter(d => d.tipo === 'H');
                    const maxFilas = Math.max(movimientosDebe.length, movimientosHaber.length);

                    const dataMovimientos = [];

                    for (let i = 0; i < maxFilas; i++) {
                        const debe = movimientosDebe[i];
                        const haber = movimientosHaber[i];

                        dataMovimientos.push([
                            debe ? `Asiento #${debe.idPartida}` : '',
                            debe ? '$' + debe.monto.toFixed(2) : '',
                            haber ? `Asiento #${haber.idPartida}` : '',
                            haber ? '$' + haber.monto.toFixed(2) : ''
                        ]);
                    }

                    // Agregar totales
                    dataMovimientos.push([
                        'TOTAL DEBE',
                        '$' + cuenta.totalDebe.toFixed(2),
                        'TOTAL HABER',
                        '$' + cuenta.totalHaber.toFixed(2)
                    ]);

                    // Agregar saldo final
                    const saldoFinal = Math.abs(cuenta.totalDebe - cuenta.totalHaber);
                    const ladoSaldo = cuenta.totalDebe > cuenta.totalHaber ? 'DEUDOR' : 'ACREEDOR';
                    dataMovimientos.push([
                        { content: `SALDO FINAL: $${saldoFinal.toFixed(2)} (${ladoSaldo})`, colSpan: 4, styles: { halign: 'center', fontStyle: 'bold', fillColor: [255, 243, 205] } }
                    ]);

                    doc.autoTable({
                        head: [['DEBE', 'Monto', 'HABER', 'Monto']],
                        body: dataMovimientos,
                        startY: yPosition,
                        margin: { left: margin, right: margin },
                        columnStyles: {
                            0: { halign: 'left', cellWidth: 45 },
                            1: { halign: 'right', cellWidth: 35 },
                            2: { halign: 'left', cellWidth: 45 },
                            3: { halign: 'right', cellWidth: 35 }
                        },
                        headStyles: {
                            fillColor: [236, 240, 241],
                            textColor: [0, 0, 0],
                            fontStyle: 'bold'
                        },
                        styles: {
                            fontSize: 9,
                            cellPadding: 3
                        },
                        didDrawPage: function(data) {
                            yPosition = data.cursor.y + 5;
                        }
                    });

                    yPosition = doc.lastAutoTable.finalY + 12;
                });

                // Mostrar cuentas SIN movimiento en tabla simple
                if (cuentasSinMovimiento.length > 0) {
                    if (yPosition > pageHeight - 50) {
                        doc.addPage();
                        yPosition = 20;
                    }

                    doc.setFontSize(12);
                    doc.setFont(undefined, 'bold');
                    doc.text('Cuentas sin Movimiento', margin, yPosition);
                    yPosition += 8;

                    const dataSinMovimiento = cuentasSinMovimiento.map(cuenta => [
                        cuenta.nombre,
                        cuenta.tipo,
                        cuenta.naturaleza === 'D' ? 'Deudora' : 'Acreedora',
                        '$' + cuenta.totalDebe.toFixed(2),
                        '$' + cuenta.totalHaber.toFixed(2),
                        '$' + cuenta.saldo.toFixed(2)
                    ]);

                    doc.autoTable({
                        head: [['Cuenta', 'Tipo', 'Naturaleza', 'Total Debe', 'Total Haber', 'Saldo']],
                        body: dataSinMovimiento,
                        startY: yPosition,
                        margin: { left: margin, right: margin },
                        columnStyles: {
                            3: { halign: 'right' },
                            4: { halign: 'right' },
                            5: { halign: 'right' }
                        },
                        styles: {
                            fontSize: 9,
                            cellPadding: 3
                        },
                        didDrawPage: function(data) {
                            yPosition = data.cursor.y + 5;
                        }
                    });
                    yPosition = doc.lastAutoTable.finalY + 15;
                }
            }

            // --- Balance de Comprobación ---
            if (reporteData.balanceComprobacion && reporteData.balanceComprobacion.length > 0) {
                if (yPosition > pageHeight - 50) {
                    doc.addPage();
                    yPosition = 20;
                }

                doc.setFontSize(14);
                doc.setFont(undefined, 'bold');
                doc.text('Balance de Comprobación', margin, yPosition);
                yPosition += 10;

                const dataBC = reporteData.balanceComprobacion.map(cuenta => [
                    cuenta.nombre,
                    cuenta.tipo,
                    '$' + (cuenta.debe ? parseFloat(cuenta.debe).toFixed(2) : '0.00'),
                    '$' + (cuenta.haber ? parseFloat(cuenta.haber).toFixed(2) : '0.00')
                ]);

                // Totales
                if (reporteData.totalesBalanceComprobacion) {
                    dataBC.push([
                        'TOTALES',
                        '',
                        '$' + (reporteData.totalesBalanceComprobacion.debe ? reporteData.totalesBalanceComprobacion.debe.toFixed(2) : '0.00'),
                        '$' + (reporteData.totalesBalanceComprobacion.haber ? reporteData.totalesBalanceComprobacion.haber.toFixed(2) : '0.00')
                    ]);
                }

                doc.autoTable({
                    head: [['Cuenta', 'Tipo', 'Debe', 'Haber']],
                    body: dataBC,
                    startY: yPosition,
                    margin: { left: margin, right: margin },
                    columnStyles: {
                        2: { halign: 'right' },
                        3: { halign: 'right' }
                    },
                    didDrawPage: function(data) {
                        yPosition = data.cursor.y + 5;
                    }
                });
                yPosition = doc.lastAutoTable.finalY + 15;
            }

            // --- Balance General ---
            if (reporteData.activos || reporteData.pasivos || reporteData.capital) {
                if (yPosition > pageHeight - 50) {
                    doc.addPage();
                    yPosition = 20;
                }

                doc.setFontSize(14);
                doc.setFont(undefined, 'bold');
                doc.text('Balance General', margin, yPosition);
                yPosition += 10;

                const dataBalanceGeneral = [];

                // Helper to add rows
                const addRows = (items, sectionName) => {
                    if (items && items.length > 0) {
                        let total = 0;
                        items.forEach(item => {
                            const val = parseFloat(item.saldo) || 0;
                            total += val;
                            dataBalanceGeneral.push([sectionName, item.nombre, '$' + val.toFixed(2)]);
                        });
                        dataBalanceGeneral.push([sectionName, 'TOTAL ' + sectionName, '$' + total.toFixed(2)]);
                    }
                };

                addRows(reporteData.activos, 'ACTIVOS');
                addRows(reporteData.pasivos, 'PASIVOS');
                addRows(reporteData.capital, 'CAPITAL');

                doc.autoTable({
                    head: [['Sección', 'Cuenta', 'Saldo']],
                    body: dataBalanceGeneral,
                    startY: yPosition,
                    margin: { left: margin, right: margin },
                    columnStyles: {
                        2: { halign: 'right' }
                    }
                });
            }

            // Guardar PDF
            const filename = 'reporte_' + new Date().toISOString().split('T')[0] + '.pdf';
            doc.save(filename);

            Swal.close();
            Swal.fire({
                icon: 'success',
                title: 'Éxito',
                text: 'PDF descargado correctamente'
            });

        } catch (error) {
            console.error('[v0] Error en exportarPDF:', error);
            Swal.close();
            Swal.fire({
                icon: 'error',
                title: 'Error al generar PDF',
                text: error.message || 'Ocurrió un error inesperado'
            });
        }
    }, 500);
}

window.addEventListener('load', function() {
    console.log("[v0] Página cargada, inicializando fechas");
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);

    document.getElementById('fechaInicio').valueAsDate = firstDay;
    document.getElementById('fechaFin').valueAsDate = today;
    console.log("[v0] Inicialización completada");
});
