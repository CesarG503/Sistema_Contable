document.addEventListener("DOMContentLoaded", () => {
    const searchInput = document.getElementById("numeroAsiento")
    const fechaInicial = document.getElementById("fechaInicial")
    const fechaFinal = document.getElementById("fechaFinal")

    function filterByDateRange() {
        const startDate = fechaInicial?.value
        const endDate = fechaFinal?.value
        const tableRows = document.querySelectorAll(".table-section tbody tr")

        if (!startDate && !endDate) {
            // Show all rows if no date filter
            tableRows.forEach((row) => {
                row.style.display = ""
            })
            return
        }

        // Group rows by asiento number
        const asientoGroups = new Map()

        tableRows.forEach((row) => {
            const asientoCell = row.querySelector("td:first-child")
            const fechaCell = row.querySelector("td:nth-child(2)")

            if (asientoCell && fechaCell) {
                const asientoNumber = asientoCell.textContent.trim()
                const fechaText = fechaCell.textContent.trim()

                if (!asientoGroups.has(asientoNumber)) {
                    asientoGroups.set(asientoNumber, {
                        fecha: fechaText,
                        rows: [],
                    })
                }
                asientoGroups.get(asientoNumber).rows.push(row)
            }
        })

        // Filter by date range
        asientoGroups.forEach((group) => {
            const fechaParts = group.fecha.split("/")
            if (fechaParts.length === 3) {
                const fechaAsiento = new Date(fechaParts[2], fechaParts[1] - 1, fechaParts[0])

                let showGroup = true

                if (startDate) {
                    const start = new Date(startDate)
                    if (fechaAsiento < start) showGroup = false
                }

                if (endDate) {
                    const end = new Date(endDate)
                    if (fechaAsiento > end) showGroup = false
                }

                group.rows.forEach((row) => {
                    row.style.display = showGroup ? "" : "none"
                })
            }
        })
    }

    if (searchInput) {
        searchInput.addEventListener("keyup", function () {
            const searchValue = this.value.toLowerCase().trim()
            const tableRows = document.querySelectorAll(".table-section tbody tr")

            if (searchValue === "") {
                // Reapply date filter if search is empty
                filterByDateRange()
                return
            }

            // Group rows by asiento number
            const asientoGroups = new Map()

            tableRows.forEach((row) => {
                const asientoCell = row.querySelector("td:first-child")
                if (asientoCell) {
                    const asientoNumber = asientoCell.textContent.trim()
                    if (!asientoGroups.has(asientoNumber)) {
                        asientoGroups.set(asientoNumber, [])
                    }
                    asientoGroups.get(asientoNumber).push(row)
                }
            })

            // Filter and show/hide rows based on search
            asientoGroups.forEach((rows, asientoNumber) => {
                const matches = asientoNumber.toLowerCase().includes(searchValue)
                rows.forEach((row) => {
                    row.style.display = matches ? "" : "none"
                })
            })
        })
    }

    if (fechaInicial) {
        fechaInicial.addEventListener("change", filterByDateRange)
    }

    if (fechaFinal) {
        fechaFinal.addEventListener("change", filterByDateRange)
    }
})
