document.addEventListener("DOMContentLoaded", () => {
    initializeFilters()
    setupSearchListener()
    setupTypeFilterListener()
})

function initializeFilters() {
    const accounts = document.querySelectorAll(".cuenta-card")
    const tipos = new Set()

    accounts.forEach((card) => {
        const tipo = card.getAttribute("data-tipo")
        if (tipo && tipo.trim() !== "") {
            tipos.add(tipo)
        }
    })

    // Populate type filter dropdown
    const filterSelect = document.getElementById("filterTipo")
    const sortedTipos = Array.from(tipos).sort()

    sortedTipos.forEach((tipo) => {
        const option = document.createElement("option")
        option.value = tipo
        option.textContent = tipo
        filterSelect.appendChild(option)
    })

    updateCuentasCount()
}

function setupSearchListener() {
    const searchInput = document.getElementById("searchCuentas")
    let searchTimeout

    searchInput.addEventListener("input", () => {
        clearTimeout(searchTimeout)
        searchTimeout = setTimeout(() => {
            filterCuentas()
        }, 300)
    })
}

function setupTypeFilterListener() {
    const filterSelect = document.getElementById("filterTipo")
    filterSelect.addEventListener("change", () => {
        filterCuentas()
    })
}

function filterCuentas() {
    const searchTerm = document.getElementById("searchCuentas").value.toLowerCase()
    const selectedTipo = document.getElementById("filterTipo").value
    const accounts = document.querySelectorAll(".cuenta-card")
    let visibleCount = 0

    accounts.forEach((card) => {
        const nombre = card.getAttribute("data-nombre").toLowerCase()
        const numero = (card.getAttribute("data-numero") || "").toLowerCase()
        const tipo = card.getAttribute("data-tipo")

        // Check if account matches search term
        const matchesSearch = nombre.includes(searchTerm) || numero.includes(searchTerm)

        // Check if account matches type filter
        const matchesType = selectedTipo === "" || tipo === selectedTipo

        // Show/hide based on both filters
        if (matchesSearch && matchesType) {
            card.style.display = ""
            visibleCount++
        } else {
            card.style.display = "none"
        }
    })

    updateCuentasCount()
}

function updateCuentasCount() {
    const accounts = document.querySelectorAll(".cuenta-card")
    let visibleCount = 0

    accounts.forEach((card) => {
        if (card.style.display !== "none") {
            visibleCount++
        }
    })

    document.getElementById("cuentasCount").textContent = visibleCount
}

function resetFilters() {
    document.getElementById("searchCuentas").value = ""
    document.getElementById("filterTipo").value = ""
    filterCuentas()
}

function abrirModalDetalle(idCuenta) {
    fetch(`/cuentas/detalle/${idCuenta}`)
        .then((response) => response.json())
        .then((data) => {
            if (data.error) {
                alert(data.error)
                return
            }

            const numeroCuenta = data.cuenta.numeroCuenta || ""
            document.getElementById("cuentaNombre").textContent = numeroCuenta
                ? `${numeroCuenta} - ${data.cuenta.nombre}`
                : data.cuenta.nombre

            document.getElementById("cuentaDescripcion").textContent = data.cuenta.descripcion || ""
            const tipo = data.cuenta.tipo ? data.cuenta.tipo + ": " : ""
            const naturaleza = data.cuenta.naturaleza === "D" ? "Deudora" : "Acreedora"
            const tipoNaturalezaSpan = document.getElementById("cuentaTipoNaturaleza")
            tipoNaturalezaSpan.textContent = tipo + naturaleza
            tipoNaturalezaSpan.className =
                "cuenta-naturaleza " + (data.cuenta.naturaleza === "D" ? "naturaleza-D" : "naturaleza-A")

            document.getElementById("debitoList").innerHTML = ""
            document.getElementById("creditoList").innerHTML = ""

            let totalDebito = 0
            let totalCredito = 0

            data.movimientos.forEach((mov) => {
                const entry = document.createElement("div")
                entry.className = "taccount-entry"

                const asiento = document.createElement("span")
                asiento.className = "taccount-asiento"
                asiento.textContent = mov.numeroAsiento || "-"

                const monto = document.createElement("span")
                monto.className = "taccount-monto"
                monto.textContent = `$ ${Number.parseFloat(mov.monto).toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

                entry.appendChild(asiento)
                entry.appendChild(monto)

                if (mov.tipo === "D") {
                    document.getElementById("debitoList").appendChild(entry)
                    totalDebito += Number.parseFloat(mov.monto)
                } else {
                    document.getElementById("creditoList").appendChild(entry)
                    totalCredito += Number.parseFloat(mov.monto)
                }
            })

            document.getElementById("modalTotalDebe").textContent =
                `$ ${totalDebito.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
            document.getElementById("modalTotalHaber").textContent =
                `$ ${totalCredito.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
            const saldoFinal = data.cuenta.naturaleza === "D" ? totalDebito - totalCredito : totalCredito - totalDebito
            document.getElementById("modalSaldoFinal").textContent =
                `$ ${Math.abs(saldoFinal).toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

            document.getElementById("modalDetalleCuenta").style.display = "flex"
        })
        .catch((error) => {
            console.error("Error:", error)
            alert("Error al cargar los detalles de la cuenta")
        })
}

function cerrarModal(event) {
    if (!event || event.target.classList.contains("modal-overlay") || event.target.classList.contains("modal-close")) {
        document.getElementById("modalDetalleCuenta").style.display = "none"
    }
}

function cerrarAlerta(button) {
    const alert = button.closest(".alert")
    alert.style.animation = "slideOut 0.3s ease"

    setTimeout(() => {
        alert.style.display = "none"
    }, 300)
}
