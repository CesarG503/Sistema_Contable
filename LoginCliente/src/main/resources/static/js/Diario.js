function openModal() {
    document.getElementById("partidaModal").style.display = "block"
}

function closeModal() {
    document.getElementById("partidaModal").style.display = "none"
    document.getElementById("partidaForm").reset()
}

function addMovimiento() {
    const container = document.getElementById("movimientos")
    const firstRow = container.querySelector(".movimiento-row")
    const newRow = firstRow.cloneNode(true)
    newRow.querySelectorAll("input, select").forEach((input) => (input.value = ""))
    container.appendChild(newRow)
    attachCalculateListeners()
}

function removeMovimiento(btn) {
    const container = document.getElementById("movimientos")
    if (container.querySelectorAll(".movimiento-row").length > 2) {
        btn.closest(".movimiento-row").remove()
        calculateTotals()
    }
}

function calculateTotals() {
    let totalDebe = 0
    let totalHaber = 0

    document.querySelectorAll(".movimiento-row").forEach((row) => {
        const monto = Number.parseFloat(row.querySelector(".monto-input").value) || 0
        const tipo = row.querySelector(".tipo-select").value

        if (tipo === "D") {
            totalDebe += monto
        } else if (tipo === "H") {
            totalHaber += monto
        }
    })

    document.getElementById("totalDebe").textContent = "$" + totalDebe.toFixed(2)
    document.getElementById("totalHaber").textContent = "$" + totalHaber.toFixed(2)
    document.getElementById("diferencia").textContent = "$" + Math.abs(totalDebe - totalHaber).toFixed(2)
}

function attachCalculateListeners() {
    document.querySelectorAll(".monto-input, .tipo-select").forEach((element) => {
        element.removeEventListener("input", calculateTotals)
        element.addEventListener("input", calculateTotals)
    })
}

attachCalculateListeners()

document.getElementById("partidaForm").addEventListener("submit", async (e) => {
    e.preventDefault()

    const concepto = document.getElementById("concepto").value
    const fechaPartida = document.getElementById("fechaPartida").value
    const movimientos = []

    document.querySelectorAll(".movimiento-row").forEach((row) => {
        const idCuenta = row.querySelector(".cuenta-select").value
        const monto = row.querySelector(".monto-input").value
        const tipo = row.querySelector(".tipo-select").value

        if (idCuenta && monto && tipo) {
            movimientos.push({ idCuenta, monto, tipo })
        }
    })

    try {
        const response = await fetch("/partidas/crear", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ concepto, fechaPartida, movimientos }),
        })

        const data = await response.json()

        if (response.ok) {
            alert("Partida creada exitosamente")
            location.reload()
        } else {
            alert(data.error || "Error al crear la partida")
        }
    } catch (error) {
        alert("Error al crear la partida: " + error.message)
    }
})
