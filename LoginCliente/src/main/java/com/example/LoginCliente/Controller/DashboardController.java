package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Service.UsuarioService;
import com.example.LoginCliente.Service.PartidaService;
import com.example.LoginCliente.Service.CuentaService;
import com.example.LoginCliente.Models.Partida;
import com.example.LoginCliente.Models.PartidaDTO;
import com.example.LoginCliente.Models.Cuenta;
import com.example.LoginCliente.Models.Movimiento;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private PartidaService partidaService;
    @Autowired
    private CuentaService cuentaService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuario = usuarioService.findByUsuario(username);
        model.addAttribute("usuario", usuario);
        // Obtener partidas y cuentas igual que en libro-diario
        List<Partida> partidas = partidaService.findAll();
        List<Cuenta> cuentas = cuentaService.findAll();
        Map<PartidaDTO, List<Movimiento>> partidasConMovimientos = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Partida partida : partidas) {
            String fechaFormateada = "";
            if (partida.getFecha() != null) {
                if (partida.getFecha() instanceof Timestamp) {
                    fechaFormateada = ((Timestamp) partida.getFecha()).toLocalDateTime().format(formatter);
                } else if (partida.getFecha() instanceof java.util.Date) {
                    fechaFormateada = new Timestamp(((java.util.Date) partida.getFecha()).getTime()).toLocalDateTime().format(formatter);
                } else {
                    fechaFormateada = partida.getFecha().toString();
                }
            }
            String autorStr = partida.getAutor() != null ? partida.getAutor().toString() : "";
            PartidaDTO partidaDTO = new PartidaDTO(
                partida.getIdPartida(),
                partida.getConcepto(),
                fechaFormateada,
                autorStr
            );
            List<Movimiento> movimientos = partidaService.findMovimientosByPartida(partida.getIdPartida());
            partidasConMovimientos.put(partidaDTO, movimientos);
        }
        model.addAttribute("partidasConMovimientos", partidasConMovimientos);
        model.addAttribute("cuentas", cuentas);
        model.addAttribute("page", "dashboard");
        return "dashboard";
    }
}
