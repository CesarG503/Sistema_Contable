package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.*;
import com.example.LoginCliente.Service.UsuarioService;
import com.example.LoginCliente.Service.PartidaService;
import com.example.LoginCliente.Service.CuentaService;
import com.example.LoginCliente.Service.UsuarioEmpresaService;
import com.example.LoginCliente.Service.DocumentosPartidaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    @Autowired
    private UsuarioEmpresaService usuarioEmpresaService;
    @Autowired
    private DocumentosPartidaService documentosPartidaService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Usuario usuario = usuarioService.findByUsuario(username);
        model.addAttribute("usuario", usuario);

        List<UsuarioEmpresa> usuarioEmpresas = usuarioEmpresaService.findByIdUsuario(usuario.getIdUsuario());
        model.addAttribute("usuarioEmpresas", usuarioEmpresas);

        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");

        if (empresaActiva == null) {
            if (!usuarioEmpresas.isEmpty()) {
                model.addAttribute("ErrorMessage", "Por favor, selecciona una empresa para continuar.");
                return "redirect:/empresas/mis-empresas";
            } else {
                // User has no companies, show empty dashboard
                model.addAttribute("page", "dashboard");
                model.addAttribute("noCompanies", true);
                return "dashboard";
            }
        }

        Map<String, BigDecimal> ecuacionContable = cuentaService.calcularEcuacionContable(empresaActiva);
        model.addAttribute("totalActivo", ecuacionContable.get("activo"));
        model.addAttribute("totalPasivo", ecuacionContable.get("pasivo"));
        model.addAttribute("totalCapital", ecuacionContable.get("capital"));

        List<Partida> partidas = partidaService.findByIdEmpresa(empresaActiva);
        List<Cuenta> cuentas = cuentaService.findByIdEmpresa(empresaActiva);

        Map<PartidaDTO, List<Movimiento>> partidasConMovimientos = new LinkedHashMap<>();
        Map<Integer, List<DocumentosFuenteDTO>> documentosPorPartida = new HashMap<>();
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

            // Obtener documentos de la partida
            List<DocumentosFuente> documentos = documentosPartidaService.findDocumentosByPartidaId(partida.getIdPartida());
            List<DocumentosFuenteDTO> documentosDTO = new ArrayList<>();
            documentos.forEach(documento -> {
                documentosDTO.add(
                    new DocumentosFuenteDTO(
                        documento.getId_documento(),
                        documento.getNombre(),
                        documento.getRuta(),
                        documento.getFecha_subida().toString(),
                        documento.getValor(),
                        documento.getAñadidoPor().getUsuario()
                    ));
            });

            PartidaDTO partidaDTO = new PartidaDTO(
                    partida.getIdPartida(),
                    partida.getConcepto(),
                    fechaFormateada,
                    autorStr
            );
            List<Movimiento> movimientos = partidaService.findMovimientosByPartida(partida.getIdPartida());
            partidasConMovimientos.put(partidaDTO, movimientos);
            documentosPorPartida.put(partida.getIdPartida(), documentosDTO);
        }

        // Convertir documentosPorPartida a JSON
        ObjectMapper mapper = new ObjectMapper();
        String documentosJson = "{}";
        try {
            documentosJson = mapper.writeValueAsString(documentosPorPartida);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        model.addAttribute("partidasConMovimientos", partidasConMovimientos);
        model.addAttribute("documentosPorPartida", documentosJson);
        model.addAttribute("cuentas", cuentas);

        model.addAttribute("page", "dashboard");
        return "dashboard";
    }
}
