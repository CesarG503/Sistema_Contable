let reporteData = null;
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
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: #999;">No hay partidas en este período</td></tr>';
        return;
    }

    data.libroDiario.forEach(partida => {
        const fila = document.createElement('tr');
        fila.innerHTML = `
                <td>${partida.idPartida}</td>
                <td>${partida.fecha}</td>
                <td>${partida.concepto || '---'}</td>
                <td class="text-right">$${partida.totalDebe.toFixed(2)}</td>
                <td class="text-right">$${partida.totalHaber.toFixed(2)}</td>
            `;
        tbody.appendChild(fila);

        // Agregar fila para movimientos detallados
        partida.movimientos.forEach(mov => {
            const filaDetalle = document.createElement('tr');
            filaDetalle.style.backgroundColor = '#f9f9f9';
            filaDetalle.innerHTML = `
                    <td colspan="2"></td>
                    <td style="color: #666; font-style: italic;">↳ ${mov.nombreCuenta}</td>
                    <td class="text-right">${mov.tipo === 'D' ? '$' + mov.monto.toFixed(2) : '---'}</td>
                    <td class="text-right">${mov.tipo === 'H' ? '$' + mov.monto.toFixed(2) : '---'}</td>
                `;
            tbody.appendChild(filaDetalle);
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
        let detallesHTML = '';
        if (cuenta.detalles && cuenta.detalles.length > 0) {
            detallesHTML = '<div class="account-details"><table class="details-table"><thead><tr><th>Asiento</th><th>Tipo</th><th>Monto</th></tr></thead><tbody>';
            cuenta.detalles.forEach(detalle => {
                const tipoLabel = detalle.tipo === 'D' ? 'Debe' : 'Haber';
                const tipoColor = detalle.tipo === 'D' ? '#d32f2f' : '#388e3c';
                detallesHTML += `<tr><td>${detalle.idPartida}</td><td style="color: ${tipoColor}; font-weight: 600;">${tipoLabel}</td><td>$${detalle.monto.toFixed(2)}</td></tr>`;
            });
            detallesHTML += '</tbody></table></div>';
        }

        const cuentaHTML = `
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
                    ${detallesHTML}
                </div>
            `;
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
                    <div style="font-size: 12px; color: #666; font-weight: 600;">Número de Cuentas con Movimiento</div>
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

function abrirModalExportacion(tipo) {
    console.log("[v0] Abriendo modal de exportacion, tipo:", tipo);
    console.log("[v0] reporteData existe:", reporteData !== null);

    if (!reporteData) {
        Swal.fire({
            icon: 'warning',
            title: 'Sin datos',
            text: 'Por favor genera un reporte primero'
        });
        console.error('[v0] No hay reporteData');
        return;
    }

    tipoExportacionActual = tipo;
    const modal = document.getElementById('modalExportacion');
    const overlay = document.getElementById('modalOverlay');

    console.log("[v0] Modal elemento:", modal);
    console.log("[v0] Overlay elemento:", overlay);

    if (modal) {
        modal.style.display = 'block';
        console.log("[v0] Modal mostrado");
    }
    if (overlay) {
        overlay.style.display = 'block';
    }

    document.body.style.overflow = 'hidden';
}

function cerrarModalExportacion() {
    console.log("[v0] Cerrando modal");
    const modal = document.getElementById('modalExportacion');
    const overlay = document.getElementById('modalOverlay');

    if (modal) {
        modal.style.display = 'none';
    }
    if (overlay) {
        overlay.style.display = 'none';
    }

    document.body.style.overflow = 'auto';
    tipoExportacionActual = null;
}

function obtenerSeccionesSeleccionadas() {
    const checkboxes = document.querySelectorAll('input[name="secciones"]:checked');
    console.log("[v0] Checkboxes encontrados:", checkboxes.length);

    const secciones = {};

    checkboxes.forEach(checkbox => {
        console.log("[v0] Checkbox seleccionado:", checkbox.value);
        secciones[checkbox.value] = true;
    });

    console.log("[v0] Secciones seleccionadas:", secciones);
    return secciones;
}

function confirmarExportacion() {
    console.log("[v0] Confirmando exportación");

    const secciones = obtenerSeccionesSeleccionadas();

    if (Object.keys(secciones).length === 0) {
        Swal.fire({
            icon: 'warning',
            title: 'Selecciona secciones',
            text: 'Por favor selecciona al menos una sección'
        });
        return;
    }

    console.log("[v0] Tipo exportación actual:", tipoExportacionActual);

    cerrarModalExportacion();

    Swal.fire({
        title: 'Generando archivo...',
        html: 'Por favor espera',
        icon: 'info',
        allowOutsideClick: false,
        allowEscapeKey: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    if (tipoExportacionActual === 'pdf') {
        console.log("[v0] Iniciando exportación PDF");
        setTimeout(() => exportarPDF(secciones), 100);
    } else if (tipoExportacionActual === 'excel') {
        console.log("[v0] Iniciando exportación Excel");
        setTimeout(() => exportarExcel(secciones), 100);
    }
}

function exportarPDF(secciones = null) {
    console.log("[v0] Iniciando exportación con jsPDF, secciones:", secciones);

    if (!reporteData) {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Por favor genera un reporte primero'
        });
        return;
    }

    if (!secciones) {
        secciones = {
            'partida-doble': true,
            'libro-mayor': true,
            'balance-comprobacion': true,
            'balance-general': true
        };
    }

    try {
        const { jsPDF } = window.jspdf;
        const doc = new jsPDF('p', 'mm', 'a4');
        let yPosition = 20;
        const pageHeight = doc.internal.pageSize.getHeight();
        const pageWidth = doc.internal.pageSize.getWidth();
        const margin = 15;
        const contentWidth = pageWidth - 2 * margin;

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

        // Partida Doble
        if (secciones['partida-doble'] && reporteData.libroDiario && reporteData.libroDiario.length > 0) {
            if (yPosition > pageHeight - 50) {
                doc.addPage();
                yPosition = 20;
            }

            doc.setFontSize(14);
            doc.setFont(undefined, 'bold');
            doc.text('Libro Diario (Partida Doble)', margin, yPosition);
            yPosition += 10;

            const dataDiario = reporteData.libroDiario.map(p => [
                p.idPartida,
                p.fecha,
                p.concepto || '---',
                '$' + p.totalDebe.toFixed(2),
                '$' + p.totalHaber.toFixed(2)
            ]);

            doc.autoTable({
                head: [['# Asiento', 'Fecha', 'Descripción', 'Debe', 'Haber']],
                body: dataDiario,
                startY: yPosition,
                margin: { left: margin, right: margin },
                columnStyles: {
                    0: { halign: 'center' },
                    1: { halign: 'center' },
                    3: { halign: 'right' },
                    4: { halign: 'right' }
                },
                didDrawPage: function(data) {
                    yPosition = data.cursor.y + 5;
                }
            });

            yPosition = doc.lastAutoTable.finalY + 15;
        }

        // Libro Mayor
        if (secciones['libro-mayor'] && reporteData.libroMayor) {
            if (yPosition > pageHeight - 50) {
                doc.addPage();
                yPosition = 20;
            }

            doc.setFontSize(14);
            doc.setFont(undefined, 'bold');
            doc.text('Libro Mayor (Cuentas T)', margin, yPosition);
            yPosition += 10;

            const dataLibroMayor = [];
            Object.values(reporteData.libroMayor || {}).forEach(cuenta => {
                dataLibroMayor.push([
                    cuenta.nombre,
                    cuenta.tipo,
                    cuenta.naturaleza === 'D' ? 'Deudora' : 'Acreedora',
                    '$' + cuenta.totalDebe.toFixed(2),
                    '$' + cuenta.totalHaber.toFixed(2),
                    '$' + cuenta.saldo.toFixed(2)
                ]);
            });

            doc.autoTable({
                head: [['Cuenta', 'Tipo', 'Naturaleza', 'Total Debe', 'Total Haber', 'Saldo']],
                body: dataLibroMayor,
                startY: yPosition,
                margin: { left: margin, right: margin },
                columnStyles: {
                    0: { halign: 'left' },
                    3: { halign: 'right' },
                    4: { halign: 'right' },
                    5: { halign: 'right' }
                },
                didDrawPage: function(data) {
                    yPosition = data.cursor.y + 5;
                }
            });

            yPosition = doc.lastAutoTable.finalY + 15;
        }

        // Balance de Comprobación
        if (secciones['balance-comprobacion'] && reporteData.balanceComprobacion && reporteData.balanceComprobacion.length > 0) {
            if (yPosition > pageHeight - 50) {
                doc.addPage();
                yPosition = 20;
            }

            doc.setFontSize(14);
            doc.setFont(undefined, 'bold');
            doc.text('Balance de Comprobación', margin, yPosition);
            yPosition += 10;

            const dataBC = [];
            let totalDebe = 0;
            let totalHaber = 0;

            reporteData.balanceComprobacion.forEach(cuenta => {
                const debe = parseFloat(cuenta.debe) || 0;
                const haber = parseFloat(cuenta.haber) || 0;
                totalDebe += debe;
                totalHaber += haber;

                dataBC.push([
                    cuenta.nombre,
                    cuenta.tipo,
                    '$' + debe.toFixed(2),
                    '$' + haber.toFixed(2)
                ]);
            });

            dataBC.push([
                'TOTALES',
                '',
                '$' + totalDebe.toFixed(2),
                '$' + totalHaber.toFixed(2)
            ]);

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

        // Balance General
        if (secciones['balance-general']) {
            if (yPosition > pageHeight - 50) {
                doc.addPage();
                yPosition = 20;
            }

            doc.setFontSize(14);
            doc.setFont(undefined, 'bold');
            doc.text('Balance General', margin, yPosition);
            yPosition += 10;

            let totalActivos = 0;
            let totalPasivos = 0;
            let totalCapital = 0;

            if (reporteData.activos) {
                reporteData.activos.forEach(a => {
                    totalActivos += parseFloat(a.saldo) || 0;
                });
            }
            if (reporteData.pasivos) {
                reporteData.pasivos.forEach(p => {
                    totalPasivos += parseFloat(p.saldo) || 0;
                });
            }
            if (reporteData.capital) {
                reporteData.capital.forEach(c => {
                    totalCapital += parseFloat(c.saldo) || 0;
                });
            }

            const dataBalanceGeneral = [];

            if (reporteData.activos && reporteData.activos.length > 0) {
                reporteData.activos.forEach(a => {
                    dataBalanceGeneral.push(['ACTIVOS', a.nombre, '$' + parseFloat(a.saldo).toFixed(2)]);
                });
                dataBalanceGeneral.push(['ACTIVOS', 'Total Activos', '$' + totalActivos.toFixed(2)]);
            }

            if (reporteData.pasivos && reporteData.pasivos.length > 0) {
                reporteData.pasivos.forEach(p => {
                    dataBalanceGeneral.push(['PASIVOS', p.nombre, '$' + parseFloat(p.saldo).toFixed(2)]);
                });
                dataBalanceGeneral.push(['PASIVOS', 'Total Pasivos', '$' + totalPasivos.toFixed(2)]);
            }

            if (reporteData.capital && reporteData.capital.length > 0) {
                reporteData.capital.forEach(c => {
                    dataBalanceGeneral.push(['CAPITAL', c.nombre, '$' + parseFloat(c.saldo).toFixed(2)]);
                });
                dataBalanceGeneral.push(['CAPITAL', 'Total Capital', '$' + totalCapital.toFixed(2)]);
            }

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
            text: 'PDF generado y descargado correctamente'
        });

    } catch (error) {
        Swal.close();
        console.error('[v0] Error en exportarPDF:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error al generar PDF: ' + error.message
        });
    }
}

function exportarExcel(secciones = null) {
    console.log("[v0] Función exportarExcel llamada con secciones:", secciones);

    if (!reporteData) {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Por favor genera un reporte primero'
        });
        return;
    }

    if (!secciones) {
        secciones = {
            'partida-doble': true,
            'libro-mayor': true,
            'balance-comprobacion': true,
            'balance-general': true
        };
    }

    if (typeof XLSX === 'undefined') {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error: La librería XLSX no se cargó correctamente'
        });
        return;
    }

    try {
        const workbook = XLSX.utils.book_new();

        if (secciones['partida-doble'] && reporteData.libroDiario && reporteData.libroDiario.length > 0) {
            const dataDiario = reporteData.libroDiario.map(p => ({
                '# Asiento': p.idPartida,
                'Fecha': p.fecha,
                'Descripción': p.concepto || '---',
                'Debe': p.totalDebe,
                'Haber': p.totalHaber
            }));
            const ws1 = XLSX.utils.json_to_sheet(dataDiario);
            ws1['!cols'] = [
                { wch: 10 },
                { wch: 12 },
                { wch: 25 },
                { wch: 12 },
                { wch: 12 }
            ];
            XLSX.utils.book_append_sheet(workbook, ws1, 'Libro Diario');
        }

        if (secciones['libro-mayor'] && reporteData.libroMayor) {
            const dataLibroMayor = [];
            Object.values(reporteData.libroMayor || {}).forEach((cuenta, index) => {
                dataLibroMayor.push({
                    'Cuenta': cuenta.nombre,
                    'Tipo': cuenta.tipo,
                    'Naturaleza': cuenta.naturaleza === 'D' ? 'Deudora' : 'Acreedora',
                    'Total Debe': cuenta.totalDebe,
                    'Total Haber': cuenta.totalHaber,
                    'Saldo': cuenta.saldo
                });
            });
            const ws2 = XLSX.utils.json_to_sheet(dataLibroMayor);
            ws2['!cols'] = [
                { wch: 20 },
                { wch: 12 },
                { wch: 12 },
                { wch: 12 },
                { wch: 12 },
                { wch: 12 }
            ];
            XLSX.utils.book_append_sheet(workbook, ws2, 'Libro Mayor');
        }

        if (secciones['balance-comprobacion'] && reporteData.balanceComprobacion && reporteData.balanceComprobacion.length > 0) {
            const dataBC = reporteData.balanceComprobacion.map(c => ({
                'Cuenta': c.nombre,
                'Tipo': c.tipo,
                'Debe': c.debe,
                'Haber': c.haber
            }));

            let totalDebe = 0;
            let totalHaber = 0;
            reporteData.balanceComprobacion.forEach(c => {
                totalDebe += parseFloat(c.debe) || 0;
                totalHaber += parseFloat(c.haber) || 0;
            });
            dataBC.push({
                'Cuenta': 'TOTALES',
                'Tipo': '',
                'Debe': totalDebe,
                'Haber': totalHaber
            });

            const ws3 = XLSX.utils.json_to_sheet(dataBC);
            ws3['!cols'] = [
                { wch: 25 },
                { wch: 12 },
                { wch: 12 },
                { wch: 12 }
            ];
            XLSX.utils.book_append_sheet(workbook, ws3, 'Balance Comprobación');
        }

        if (secciones['balance-general']) {
            const dataBalanceGeneral = [];

            if (reporteData.activos && reporteData.activos.length > 0) {
                let totalActivos = 0;
                reporteData.activos.forEach(a => {
                    totalActivos += parseFloat(a.saldo) || 0;
                    dataBalanceGeneral.push({
                        'Sección': 'ACTIVOS',
                        'Cuenta': a.nombre,
                        'Saldo': parseFloat(a.saldo)
                    });
                });
                dataBalanceGeneral.push({
                    'Sección': 'ACTIVOS',
                    'Cuenta': 'Total Activos',
                    'Saldo': totalActivos
                });
            }

            if (reporteData.pasivos && reporteData.pasivos.length > 0) {
                let totalPasivos = 0;
                reporteData.pasivos.forEach(p => {
                    totalPasivos += parseFloat(p.saldo) || 0;
                    dataBalanceGeneral.push({
                        'Sección': 'PASIVOS',
                        'Cuenta': p.nombre,
                        'Saldo': parseFloat(p.saldo)
                    });
                });
                dataBalanceGeneral.push({
                    'Sección': 'PASIVOS',
                    'Cuenta': 'Total Pasivos',
                    'Saldo': totalPasivos
                });
            }

            if (reporteData.capital && reporteData.capital.length > 0) {
                let totalCapital = 0;
                reporteData.capital.forEach(c => {
                    totalCapital += parseFloat(c.saldo) || 0;
                    dataBalanceGeneral.push({
                        'Sección': 'CAPITAL',
                        'Cuenta': c.nombre,
                        'Saldo': parseFloat(c.saldo)
                    });
                });
                dataBalanceGeneral.push({
                    'Sección': 'CAPITAL',
                    'Cuenta': 'Total Capital',
                    'Saldo': totalCapital
                });
            }

            const ws4 = XLSX.utils.json_to_sheet(dataBalanceGeneral);
            ws4['!cols'] = [
                { wch: 15 },
                { wch: 25 },
                { wch: 12 }
            ];
            XLSX.utils.book_append_sheet(workbook, ws4, 'Balance General');
        }

        const filename = 'reporte_' + new Date().toISOString().split('T')[0] + '.xlsx';
        XLSX.writeFile(workbook, filename);

        Swal.close();
        Swal.fire({
            icon: 'success',
            title: 'Éxito',
            text: 'Excel generado y descargado correctamente'
        });
    } catch (error) {
        Swal.close();
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error al generar Excel: ' + error.message
        });
    }
}

window.addEventListener('load', function() {
    console.log("[v0] Página cargada, inicializando fechas");
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);

    document.getElementById('fechaInicio').valueAsDate = firstDay;
    document.getElementById('fechaFin').valueAsDate = today;
    console.log("[v0] Inicialización completada");
});
