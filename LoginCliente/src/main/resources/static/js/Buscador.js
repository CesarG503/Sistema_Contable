document.addEventListener("DOMContentLoaded", () => {
    const searchInput = document.getElementById("numeroAsiento")
    const fechaInicial = document.getElementById("fechaInicial")
    const fechaFinal = document.getElementById("fechaFinal")

    // Store all row groups for efficient filtering
    let rowGroups = []

    // Initialize row groups on page load
    function initializeRowGroups() {
        const tableRows = document.querySelectorAll(".table-section tbody tr")
        rowGroups = []
        let currentGroup = null

        tableRows.forEach((row) => {
            const firstCell = row.querySelector("td:first-child")

            // Check if this is a main row (has rowspan attribute)
            if (firstCell && firstCell.hasAttribute("rowspan")) {
                // This is the start of a new group
                const asientoNumber = firstCell.textContent.trim()
                const fechaCell = row.querySelector("td:nth-child(2)")
                const fecha = fechaCell ? fechaCell.textContent.trim() : ""

                currentGroup = {
                    asiento: asientoNumber,
                    fecha: fecha,
                    rows: [row],
                }
                rowGroups.push(currentGroup)
            } else if (currentGroup) {
                // This is a detail row, add to current group
                currentGroup.rows.push(row)
            }
        })
    }

    // Validate that end date is not before start date
    function validateDates() {
        if (fechaInicial && fechaFinal && fechaInicial.value && fechaFinal.value) {
            const startDate = new Date(fechaInicial.value)
            const endDate = new Date(fechaFinal.value)

            if (endDate < startDate) {
                alert("La fecha final no puede ser anterior a la fecha inicial")
                fechaFinal.value = ""
                return false
            }
        }
        return true
    }

    // Parse date from DD/MM/YYYY format to Date object
    function parseDate(dateString) {
        const parts = dateString.split("/")
        if (parts.length === 3) {
            // DD/MM/YYYY -> new Date(year, month-1, day)
            return new Date(parts[2], parts[1] - 1, parts[0])
        }
        return null
    }

    // Main filter function that applies all active filters
    function applyFilters() {
        const searchValue = searchInput ? searchInput.value.toLowerCase().trim() : ""
        const startDate = fechaInicial ? fechaInicial.value : ""
        const endDate = fechaFinal ? fechaFinal.value : ""

        rowGroups.forEach((group) => {
            let showGroup = true

            // Filter by asiento number
            if (searchValue !== "") {
                if (!group.asiento.toLowerCase().includes(searchValue)) {
                    showGroup = false
                }
            }

            // Filter by date range
            if (showGroup && (startDate || endDate)) {
                const fechaAsiento = parseDate(group.fecha)

                if (fechaAsiento) {
                    if (startDate) {
                        const start = new Date(startDate)
                        start.setHours(0, 0, 0, 0)
                        fechaAsiento.setHours(0, 0, 0, 0)
                        if (fechaAsiento < start) {
                            showGroup = false
                        }
                    }

                    if (showGroup && endDate) {
                        const end = new Date(endDate)
                        end.setHours(23, 59, 59, 999)
                        fechaAsiento.setHours(0, 0, 0, 0)
                        if (fechaAsiento > end) {
                            showGroup = false
                        }
                    }
                }
            }

            // Show or hide all rows in the group
            group.rows.forEach((row) => {
                row.style.display = showGroup ? "" : "none"
            })
        })
    }

    // Initialize groups when page loads
    initializeRowGroups()

    // Event listener for asiento search (real-time)
    if (searchInput) {
        searchInput.addEventListener("input", () => {
            applyFilters()
        })
    }

    // Event listener for start date
    if (fechaInicial) {
        fechaInicial.addEventListener("change", () => {
            if (validateDates()) {
                applyFilters()
            }
        })
    }

    // Event listener for end date
    if (fechaFinal) {
        fechaFinal.addEventListener("change", () => {
            if (validateDates()) {
                applyFilters()
            }
        })
    }
})
