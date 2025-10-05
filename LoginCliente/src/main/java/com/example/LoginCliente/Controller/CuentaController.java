package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Cuenta;
import com.example.LoginCliente.Models.Movimiento;
import com.example.LoginCliente.Service.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    @GetMapping("/libro-mayor")
    public String libroMayor(Model model) {
        List<Cuenta> cuentas = cuentaService.findAll();

        // Calculate balances for each account
        Map<Cuenta, Map<String, BigDecimal>> cuentasConSaldos = new HashMap<>();
        for (Cuenta cuenta : cuentas) {
            Map<String, BigDecimal> saldos = cuentaService.calcularSaldoCuenta(cuenta.getId_cuenta());
            cuentasConSaldos.put(cuenta, saldos);
        }

        model.addAttribute("cuentasConSaldos", cuentasConSaldos);

        return "libro-mayor";
    }

    @GetMapping("/crear")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("cuenta", new Cuenta());
        return "crear-cuenta";
    }

    @PostMapping("/crear")
    public String crearCuenta(@ModelAttribute Cuenta cuenta) {
        cuentaService.save(cuenta);
        return "redirect:/cuentas/libro-mayor";
    }
}

