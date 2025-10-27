function openDetailsModal(button) {
    const asiento = button.getAttribute("data-asiento")
    const fecha = button.getAttribute("data-fecha")
    const concepto = button.getAttribute("data-concepto")
    const documento = button.getAttribute("data-documento")

    // Set basic info
    document.getElementById("detalleAsiento").textContent = asiento
    document.getElementById("detalleFecha").textContent = fecha
    document.getElementById("detalleConcepto").textContent = concepto
    const textDocumentos = document.getElementById("detalleDocumento");
    textDocumentos.firstElementChild.setAttribute("href",`/${documento}`);
    textDocumentos.firstElementChild.textContent = documento ? documento.split('/').pop() : 'N/A';

    // Fetch movements data from server
    fetchMovimientosDetalles(asiento)

    // Show modal
    document.getElementById("detallesModal").style.display = "block"
}

async function fetchMovimientosDetalles(idPartida) {
    const movimientosContainer = document.getElementById("detalleMovimientos")
    movimientosContainer.innerHTML = '<p style="text-align: center; color: #666;">Cargando...</p>'

    try {
        const response = await fetch(`/partidas/${idPartida}/movimientos`)

        if (!response.ok) {
            throw new Error("Error al cargar los movimientos")
        }

        const data = await response.json()
        renderMovimientos(data)
    } catch (error) {
        console.error("Error fetching movimientos:", error)
        movimientosContainer.innerHTML =
            '<p style="text-align: center; color: #dc3545;">Error al cargar los movimientos</p>'
    }
}

function renderMovimientos(movimientos) {
    const movimientosContainer = document.getElementById("detalleMovimientos")
    movimientosContainer.innerHTML = ""

    let totalDebe = 0
    let totalHaber = 0

    // Create list of accounts with movements
    const listaCuentas = document.createElement("ul")
    listaCuentas.className = "lista-cuentas"

    movimientos.forEach((mov) => {
        const monto = Number.parseFloat(mov.monto)
        const tipo = mov.tipo === "D" ? "Debe" : "Haber"

        if (mov.tipo === "D") {
            totalDebe += monto
        } else {
            totalHaber += monto
        }

        const li = document.createElement("li")
        li.className = "cuenta-item"
        li.innerHTML = `
            <span class="cuenta-nombre">${mov.nombreCuenta}</span>
            <span class="cuenta-movimiento ${mov.tipo === "D" ? "debe" : "haber"}">
                $${monto.toFixed(2)} (${tipo})
            </span>
        `
        listaCuentas.appendChild(li)
    })

    movimientosContainer.appendChild(listaCuentas)

    // Add totals section
    const totalesDiv = document.createElement("div")
    totalesDiv.className = "movimiento-totales"
    totalesDiv.innerHTML = `
        <div class="total-row">
            <strong>Total Debe:</strong>
            <span>$${totalDebe.toFixed(2)}</span>
        </div>
        <div class="total-row">
            <strong>Total Haber:</strong>
            <span>$${totalHaber.toFixed(2)}</span>
        </div>
    `
    movimientosContainer.appendChild(totalesDiv)
}

function closeDetailsModal() {
    document.getElementById("detallesModal").style.display = "none"
}

// Close modal when clicking outside
window.addEventListener("click", (event) => {
    const modal = document.getElementById("detallesModal")
    if (event.target === modal) {
        closeDetailsModal()
    }
})
