package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Cuenta;
import com.example.LoginCliente.Models.Movimiento;
import com.example.LoginCliente.Models.Partida;
import com.example.LoginCliente.Service.CuentaService;
import com.example.LoginCliente.Service.PartidaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private PartidaService partidaService;

    @GetMapping("/libro-mayor")
    public String libroMayor(Model model, HttpSession session) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            return "redirect:/empresas/mis-empresas";
        }

        List<Cuenta> cuentas = cuentaService.findByIdEmpresa(empresaActiva);

        Map<Cuenta, Map<String, BigDecimal>> cuentasConSaldos = new HashMap<>();
        for (Cuenta cuenta : cuentas) {
            Map<String, BigDecimal> saldos = cuentaService.calcularSaldoCuenta(cuenta.getIdCuenta());
            cuentasConSaldos.put(cuenta, saldos);
        }

        model.addAttribute("cuentasConSaldos", cuentasConSaldos);
        model.addAttribute("page", "libro-mayor");
        return "libro-mayor";
    }

    @GetMapping("/crear")
    public String mostrarFormularioCrear(Model model, HttpSession session) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            return "redirect:/empresas/mis-empresas";
        }

        model.addAttribute("cuenta", new Cuenta());
        model.addAttribute("page", "crear-cuentas");
        return "crear-cuenta";
    }

    @PostMapping("/crear")
    public String crearCuenta(@ModelAttribute Cuenta cuenta, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una empresa primero");
            return "redirect:/empresas/mis-empresas";
        }

        cuenta.setIdEmpresa(empresaActiva);
        cuentaService.save(cuenta);
        return "redirect:/cuentas/libro-mayor";
    }

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public Map<String, Object> obtenerDetalleCuenta(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();

        Cuenta cuenta = cuentaService.findById(id).orElse(null);
        if (cuenta == null) {
            response.put("error", "Cuenta no encontrada");
            return response;
        }

        List<Movimiento> movimientos = cuentaService.obtenerMovimientosPorCuenta(id);

        List<Map<String, Object>> movimientosDetalle = new ArrayList<>();
        for (Movimiento mov : movimientos) {
            Map<String, Object> detalle = new HashMap<>();
            detalle.put("monto", mov.getMonto());
            detalle.put("tipo", mov.getTipo());
            Partida partida = partidaService.findById(mov.getIdPartida()).orElse(null);
            if (partida != null) {
                detalle.put("numeroAsiento", partida.getIdPartida());
                detalle.put("fecha", partida.getFecha());
                detalle.put("concepto", partida.getConcepto());
            }

            movimientosDetalle.add(detalle);
        }

        response.put("cuenta", cuenta);
        response.put("movimientos", movimientosDetalle);

        return response;
    }

    /**
     * Obtener todas las cuentas de la empresa activa en formato JSON
     */
    @GetMapping("/todas")
    @ResponseBody
    public List<Map<String, Object>> obtenerTodasLasCuentas(HttpSession session) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        List<Map<String, Object>> cuentasData = new ArrayList<>();

        if (empresaActiva == null) {
            return cuentasData;
        }

        List<Cuenta> cuentas = cuentaService.findByIdEmpresa(empresaActiva);
        for (Cuenta cuenta : cuentas) {
            Map<String, Object> cuentaData = new HashMap<>();
            cuentaData.put("idCuenta", cuenta.getIdCuenta());
            cuentaData.put("nombre", cuenta.getNombre());
            cuentaData.put("tipo", cuenta.getTipo());
            cuentasData.add(cuentaData);
        }

        return cuentasData;
    }

    @GetMapping("/cargar-cuentas")
    public String mostrarFormularioCargarCuentas(Model model, HttpSession session) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            return "redirect:/empresas/mis-empresas";
        }

        model.addAttribute("page", "cargar-cuentas");
        return "cargar-cuentas";
    }

    @PostMapping("/procesar-csv")
    @ResponseBody
    public Map<String, Object> procesarCSV(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("modo") String modo,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");

        if (empresaActiva == null) {
            response.put("success", false);
            response.put("message", "Debe seleccionar una empresa primero");
            return response;
        }

        try {
            List<Cuenta> cuentasDelCSV = cuentaService.parsearCSV(file.getInputStream());

            if ("cargar".equals(modo)) {
                // Borrar todas las cuentas de la empresa y cargar las nuevas
                cuentaService.borrarCuentasEmpresa(empresaActiva);
                int guardadas = cuentaService.guardarCuentasCSV(cuentasDelCSV, empresaActiva);
                response.put("success", true);
                response.put("message", "Se cargaron " + guardadas + " cuentas correctamente");
                response.put("cantidad", guardadas);
            } else if ("anadir".equals(modo)) {
                // Agregar solo las cuentas que no existen
                int agregadas = cuentaService.agregarCuentasCSV(cuentasDelCSV, empresaActiva);
                response.put("success", true);
                response.put("message", "Se agregaron " + agregadas + " cuentas. " +
                        (cuentasDelCSV.size() - agregadas) + " cuentas ya existían");
                response.put("cantidad", agregadas);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al procesar el archivo: " + e.getMessage());
        }

        return response;
    }
    @PostMapping("/eliminar/{id}")
    public String eliminarCuenta(@PathVariable Integer id,
                                 RedirectAttributes redirectAttributes) {
        Map<String, Object> resultado = cuentaService.deleteById(id);

        if ((Boolean) resultado.get("success")) {
            redirectAttributes.addFlashAttribute("success", resultado.get("message"));
        } else {
            redirectAttributes.addFlashAttribute("error", resultado.get("message"));
        }

        return "redirect:/cuentas/libro-mayor";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una empresa primero");
            return "redirect:/empresas/mis-empresas";
        }

        Cuenta cuenta = cuentaService.findById(id).orElse(null);
        if (cuenta == null || !cuenta.getIdEmpresa().equals(empresaActiva)) {
            redirectAttributes.addFlashAttribute("error", "Cuenta no encontrada o no pertenece a la empresa activa");
            return "redirect:/cuentas/libro-mayor";
        }

        model.addAttribute("cuenta", cuenta);
        model.addAttribute("naturalezaOriginal", cuenta.getNaturaleza());
        model.addAttribute("page", "editar-cuenta");
        return "editar-cuenta";
    }

    @PostMapping("/editar/{id}")
    public String editarCuenta(@PathVariable Integer id,
                               @ModelAttribute Cuenta cuenta,
                               @RequestParam(name = "confirmarCambio", defaultValue = "false") boolean confirmarCambio,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una empresa primero");
            return "redirect:/empresas/mis-empresas";
        }

        Cuenta cuentaExistente = cuentaService.findById(id).orElse(null);
        if (cuentaExistente == null || !cuentaExistente.getIdEmpresa().equals(empresaActiva)) {
            redirectAttributes.addFlashAttribute("error", "Cuenta no encontrada o no pertenece a la empresa activa");
            return "redirect:/cuentas/libro-mayor";
        }

        // Verificar si cambió la naturaleza
        String naturalezaOriginal = cuentaExistente.getNaturaleza();
        String nuevaNaturaleza = cuenta.getNaturaleza();

        if (!naturalezaOriginal.equals(nuevaNaturaleza)) {
            // Verificar si tiene movimientos
            List<Movimiento> movimientos = cuentaService.obtenerMovimientosPorCuenta(id);
            if (!movimientos.isEmpty() && !confirmarCambio) {
                redirectAttributes.addFlashAttribute("error", "No se puede cambiar la naturaleza porque la cuenta tiene movimientos");
                redirectAttributes.addFlashAttribute("requiereConfirmacion", true);
                redirectAttributes.addFlashAttribute("cuenta", cuenta);
                return "redirect:/cuentas/editar/" + id;
            }
        }

        // Actualizar la cuenta
        cuentaExistente.setNombre(cuenta.getNombre());
        cuentaExistente.setDescripcion(cuenta.getDescripcion());
        cuentaExistente.setTipo(cuenta.getTipo());
        cuentaExistente.setNaturaleza(cuenta.getNaturaleza());

        cuentaService.save(cuentaExistente);
        redirectAttributes.addFlashAttribute("success", "Cuenta actualizada exitosamente");
        return "redirect:/cuentas/libro-mayor";
    }
}
