package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Cuenta;
import com.example.LoginCliente.Models.Movimiento;
import com.example.LoginCliente.Models.Partida;
import com.example.LoginCliente.Service.CuentaService;
import com.example.LoginCliente.Service.PartidaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String libroMayor(Model model) {
        List<Cuenta> cuentas = cuentaService.findAll();

        Map<Cuenta, Map<String, BigDecimal>> cuentasConSaldos = new HashMap<>();
        for (Cuenta cuenta : cuentas) {
            Map<String, BigDecimal> saldos = cuentaService.calcularSaldoCuenta(cuenta.getId_cuenta());
            cuentasConSaldos.put(cuenta, saldos);
        }

        model.addAttribute("cuentasConSaldos", cuentasConSaldos);
        model.addAttribute("page", "libro-mayor");
        return "libro-mayor";
    }

    @GetMapping("/crear")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("cuenta", new Cuenta());
        model.addAttribute("page", "crear-cuentas");
        return "crear-cuenta";
    }

    @PostMapping("/crear")
    public String crearCuenta(@ModelAttribute Cuenta cuenta) {
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
}
