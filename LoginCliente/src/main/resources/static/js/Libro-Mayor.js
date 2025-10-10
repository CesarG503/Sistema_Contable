function abrirModalDetalle(idCuenta) {
    fetch(`/cuentas/detalle/${idCuenta}`)
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                alert(data.error);
                return;
            }

            document.getElementById('cuentaNombre').textContent = `${data.cuenta.nombre}`;
            document.getElementById('cuentaDescripcion').textContent = data.cuenta.descripcion || '';
            let tipo = data.cuenta.tipo ? data.cuenta.tipo + ': ' : '';
            let naturaleza = data.cuenta.naturaleza === 'D' ? 'Deudora' : 'Acreedora';
            const tipoNaturalezaSpan = document.getElementById('cuentaTipoNaturaleza');
            tipoNaturalezaSpan.textContent = tipo + naturaleza;
            tipoNaturalezaSpan.className = 'cuenta-naturaleza ' + (data.cuenta.naturaleza === 'D' ? 'naturaleza-D' : 'naturaleza-A');

            document.getElementById('debitoList').innerHTML = '';
            document.getElementById('creditoList').innerHTML = '';

            let totalDebito = 0;
            let totalCredito = 0;

            data.movimientos.forEach(mov => {
                const entry = document.createElement('div');
                entry.className = 'taccount-entry';

                const asiento = document.createElement('span');
                asiento.className = 'taccount-asiento';
                asiento.textContent = mov.numeroAsiento || '-';

                const monto = document.createElement('span');
                monto.className = 'taccount-monto';
                monto.textContent = `$ ${parseFloat(mov.monto).toLocaleString('es-MX', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;

                entry.appendChild(asiento);
                entry.appendChild(monto);

                if (mov.tipo === 'D') {
                    document.getElementById('debitoList').appendChild(entry);
                    totalDebito += parseFloat(mov.monto);
                } else {
                    document.getElementById('creditoList').appendChild(entry);
                    totalCredito += parseFloat(mov.monto);
                }
            });

            document.getElementById('modalTotalDebe').textContent = `$ ${totalDebito.toLocaleString('es-MX', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;
            document.getElementById('modalTotalHaber').textContent = `$ ${totalCredito.toLocaleString('es-MX', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;
            let saldoFinal = (data.cuenta.naturaleza === 'D') ? (totalDebito - totalCredito) : (totalCredito - totalDebito);
            document.getElementById('modalSaldoFinal').textContent = `$ ${saldoFinal.toLocaleString('es-MX', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;

            document.getElementById('modalDetalleCuenta').style.display = 'flex';
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al cargar los detalles de la cuenta');
        });
}

function cerrarModal(event) {
    if (!event || event.target.classList.contains('modal-overlay') || event.target.classList.contains('modal-close')) {
        document.getElementById('modalDetalleCuenta').style.display = 'none';
    }
}