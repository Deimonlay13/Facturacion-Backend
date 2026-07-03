package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.context.TenantContext;
import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final EntityManager entityManager;
    private final NumberFormat clpFormat = NumberFormat.getCurrencyInstance(Locale.of("es", "CL"));

    @GetMapping("/admin")
    public AdminDashboardResponse admin() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new ReglaNegocioException("Falta identificación de empresa para cargar el dashboard");
        }

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate oldDraftLimit = LocalDate.now().minusDays(7);

        BigDecimal billedThisMonth = sumDocumentsByCompany(empresaId, monthStart);
        long drafts = countDocumentsByCompanyAndStatus(empresaId, EstadoDocumento.BORRADOR);
        long issued = countDocumentsByCompanyAndStatus(empresaId, EstadoDocumento.EMITIDO);
        long errors = countDocumentsByCompanyAndSiiStatus(empresaId, List.of(EstadoDocumentoSii.ERROR, EstadoDocumentoSii.RECHAZADO));
        long activeClients = countByCompany("ClienteEntity", empresaId, "activo = true");
        long activeProducts = countByCompany("ProductoEntity", empresaId, "activo = true");

        List<MetricItem> metrics = List.of(
                new MetricItem("Facturado este mes", money(billedThisMonth), activeClients + " clientes activos", "payments", null),
                new MetricItem("Borradores", String.valueOf(drafts), "Pendientes de emisión", "edit_note", null),
                new MetricItem("Documentos emitidos", String.valueOf(issued), "Histórico de la empresa", "task_alt", null),
                new MetricItem("Errores", String.valueOf(errors), "Requieren revisión", "warning", errors > 0 ? "alert" : null)
        );

        List<DashboardDocumentItem> recentDocuments = entityManager.createQuery("""
                        SELECT d FROM DocumentoTributarioEntity d
                        WHERE d.empresa.id = :empresaId
                        ORDER BY d.id DESC
                        """, DocumentoTributarioEntity.class)
                .setParameter("empresaId", empresaId)
                .setMaxResults(5)
                .getResultList()
                .stream()
                .map(this::documentItem)
                .toList();

        List<DashboardAlertItem> alerts = new ArrayList<>();
        EmpresaEntity empresa = entityManager.find(EmpresaEntity.class, empresaId);
        if (empresa != null && (empresa.getLogo() == null || empresa.getLogo().length == 0)) {
            alerts.add(new DashboardAlertItem("Logo de empresa", "Carga el logo para que se imprima en el PDF de factura.", "image", "info"));
        }
        if (countOldDrafts(empresaId, oldDraftLimit) > 0) {
            alerts.add(new DashboardAlertItem("Borradores antiguos", "Hay documentos sin emisión desde hace más de 7 días.", "schedule", "info"));
        }

        List<FolioItem> folios = foliosByCompany(empresaId);
        if (folios.stream().anyMatch(folio -> folio.percent() < 30)) {
            alerts.add(new DashboardAlertItem("Folios bajos", "Uno o más tipos de documento están bajo el umbral recomendado.", "priority_high", "danger"));
        }
        if (alerts.isEmpty()) {
            alerts.add(new DashboardAlertItem("Operación estable", "No hay alertas críticas para esta empresa.", "verified", "info"));
        }

        return new AdminDashboardResponse(metrics, recentDocuments, alerts, folios,
                List.of(
                        new QuickActionItem("Nueva factura", "receipt_long", "/facturacion/nueva"),
                        new QuickActionItem("Nuevo cliente", "person_add", "/clientes/nuevo"),
                        new QuickActionItem("Productos", "inventory_2", "/productos"),
                        new QuickActionItem("Perfil empresa", "account_circle", "/perfil")
                ));
    }

    @GetMapping("/super-admin")
    public SuperAdminDashboardResponse superAdmin() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long totalCompanies = scalarLong("SELECT COUNT(e) FROM EmpresaEntity e");
        long activeCompanies = scalarLong("SELECT COUNT(e) FROM EmpresaEntity e WHERE e.activo = true");
        long missingLogos = scalarLong("SELECT COUNT(e) FROM EmpresaEntity e WHERE e.logo IS NULL OR e.logoContentType IS NULL");
        long newCompanies = scalarLong("SELECT COUNT(e) FROM EmpresaEntity e WHERE e.createdAt >= :monthStart", "monthStart", monthStart);
        long totalUsers = scalarLong("SELECT COUNT(u) FROM UsuarioEntity u");
        long errors = scalarLong("""
                SELECT COUNT(d) FROM DocumentoTributarioEntity d
                WHERE d.estadoSii IN :states
                """, "states", List.of(EstadoDocumentoSii.ERROR, EstadoDocumentoSii.RECHAZADO));

        List<MetricItem> metrics = List.of(
                new MetricItem("Empresas totales", String.valueOf(totalCompanies), newCompanies + " nuevas este mes", "domain", null),
                new MetricItem("Empresas activas", String.valueOf(activeCompanies), percent(activeCompanies, totalCompanies) + "% operativas", "verified", null),
                new MetricItem("Usuarios globales", String.valueOf(totalUsers), "Distribuidos por rol", "groups", null),
                new MetricItem("Sin logo", String.valueOf(missingLogos), "Pendientes de completar perfil", "image_not_supported", missingLogos > 0 ? "alert" : null)
        );

        List<CompanyRankingItem> topCompanies = topCompanies();
        List<RoleItem> roles = roles();

        List<DashboardAlertItem> alerts = new ArrayList<>();
        if (errors > 0) {
            alerts.add(new DashboardAlertItem("Documentos rechazados", errors + " documentos requieren revisión global.", "report", "danger"));
        }
        if (missingLogos > 0) {
            alerts.add(new DashboardAlertItem("Empresas incompletas", missingLogos + " empresas no tienen logo cargado.", "business", "info"));
        }
        alerts.add(new DashboardAlertItem("Auditoría disponible", "Revisa cambios recientes de usuarios, roles y documentos.", "history_edu", "info"));

        return new SuperAdminDashboardResponse(metrics, topCompanies, alerts, roles,
                List.of(
                        new QuickActionItem("Trabajar con empresa", "change_circle", "/seleccionar-empresa"),
                        new QuickActionItem("Crear empresa", "add_business", "/empresas/nueva"),
                        new QuickActionItem("Crear usuario", "person_add", "/usuarios/nuevo"),
                        new QuickActionItem("Auditoría", "history_edu", "/auditoria")
                ));
    }

    private BigDecimal sumDocumentsByCompany(Long empresaId, LocalDate desde) {
        BigDecimal value = entityManager.createQuery("""
                        SELECT COALESCE(SUM(d.montoTotal), 0)
                        FROM DocumentoTributarioEntity d
                        WHERE d.empresa.id = :empresaId
                          AND d.fechaEmision >= :desde
                        """, BigDecimal.class)
                .setParameter("empresaId", empresaId)
                .setParameter("desde", desde)
                .getSingleResult();
        return value != null ? value : BigDecimal.ZERO;
    }

    private long countDocumentsByCompanyAndStatus(Long empresaId, EstadoDocumento estado) {
        return entityManager.createQuery("""
                        SELECT COUNT(d) FROM DocumentoTributarioEntity d
                        WHERE d.empresa.id = :empresaId AND d.estado = :estado
                        """, Long.class)
                .setParameter("empresaId", empresaId)
                .setParameter("estado", estado)
                .getSingleResult();
    }

    private long countDocumentsByCompanyAndSiiStatus(Long empresaId, List<EstadoDocumentoSii> states) {
        return entityManager.createQuery("""
                        SELECT COUNT(d) FROM DocumentoTributarioEntity d
                        WHERE d.empresa.id = :empresaId AND d.estadoSii IN :states
                        """, Long.class)
                .setParameter("empresaId", empresaId)
                .setParameter("states", states)
                .getSingleResult();
    }

    private long countOldDrafts(Long empresaId, LocalDate oldDraftLimit) {
        return entityManager.createQuery("""
                        SELECT COUNT(d) FROM DocumentoTributarioEntity d
                        WHERE d.empresa.id = :empresaId
                          AND d.estado = :estado
                          AND d.fechaEmision <= :oldDraftLimit
                        """, Long.class)
                .setParameter("empresaId", empresaId)
                .setParameter("estado", EstadoDocumento.BORRADOR)
                .setParameter("oldDraftLimit", oldDraftLimit)
                .getSingleResult();
    }

    private long countByCompany(String entity, Long empresaId, String extraWhere) {
        String query = "SELECT COUNT(e) FROM " + entity + " e WHERE e.empresa.id = :empresaId";
        if (extraWhere != null && !extraWhere.isBlank()) {
            query += " AND e." + extraWhere;
        }
        return entityManager.createQuery(query, Long.class)
                .setParameter("empresaId", empresaId)
                .getSingleResult();
    }

    private List<FolioItem> foliosByCompany(Long empresaId) {
        return entityManager.createQuery("""
                        SELECT c FROM ControlFolioEntity c
                        LEFT JOIN FETCH c.tipoDocumento
                        LEFT JOIN FETCH c.cafActivo
                        WHERE c.empresa.id = :empresaId
                        ORDER BY c.tipoDocumento.codigoSii
                        """, ControlFolioEntity.class)
                .setParameter("empresaId", empresaId)
                .getResultList()
                .stream()
                .map(this::folioItem)
                .toList();
    }

    private FolioItem folioItem(ControlFolioEntity control) {
        String name = control.getTipoDocumento() != null
                ? control.getTipoDocumento().getDescripcion()
                : "Tipo de documento";
        int desde = control.getCafActivo() != null && control.getCafActivo().getRangoDesde() != null
                ? control.getCafActivo().getRangoDesde()
                : 0;
        int hasta = control.getCafActivo() != null && control.getCafActivo().getRangoHasta() != null
                ? control.getCafActivo().getRangoHasta()
                : 0;
        int used = control.getUltimoFolioUtilizado() != null ? control.getUltimoFolioUtilizado() : desde - 1;
        int total = Math.max(hasta - desde + 1, 0);
        int available = Math.max(hasta - used, 0);
        double percent = total > 0 ? Math.round((available * 1000.0 / total)) / 10.0 : 0;
        return new FolioItem(name, available, total, percent);
    }

    private DashboardDocumentItem documentItem(DocumentoTributarioEntity document) {
        String id = document.getTipoDocumento() != null && document.getTipoDocumento().getCodigoSii() != null
                ? document.getTipoDocumento().getCodigoSii() + "-" + Objects.toString(document.getFolio(), "S/F")
                : "DOC-" + document.getId();
        String client = document.getRazonSocial() != null
                ? document.getRazonSocial()
                : document.getCliente() != null ? document.getCliente().getRazonSocial() : "Sin cliente";
        String date = document.getFechaEmision() != null ? document.getFechaEmision().toString() : "Sin fecha";
        String status = document.getEstadoSii() != null
                ? document.getEstadoSii().name()
                : document.getEstado() != null ? document.getEstado().name() : "SIN_ESTADO";
        return new DashboardDocumentItem(id, client, date, money(document.getMontoTotal()), status);
    }

    private List<CompanyRankingItem> topCompanies() {
        List<Object[]> rows = entityManager.createQuery("""
                        SELECT COALESCE(e.nombreFantasia, e.razonSocial), COUNT(d), COALESCE(SUM(d.montoTotal), 0)
                        FROM DocumentoTributarioEntity d
                        JOIN d.empresa e
                        GROUP BY e.id, e.nombreFantasia, e.razonSocial
                        ORDER BY COUNT(d) DESC
                        """, Object[].class)
                .setMaxResults(5)
                .getResultList();

        long maxDocuments = rows.stream()
                .map(row -> ((Number) row[1]).longValue())
                .max(Long::compareTo)
                .orElse(0L);

        return rows.stream()
                .map(row -> {
                    long documents = ((Number) row[1]).longValue();
                    BigDecimal total = row[2] instanceof BigDecimal amount ? amount : BigDecimal.ZERO;
                    return new CompanyRankingItem(
                            Objects.toString(row[0], "Empresa"),
                            documents,
                            money(total),
                            maxDocuments > 0 ? Math.round(documents * 1000.0 / maxDocuments) / 10.0 : 0
                    );
                })
                .toList();
    }

    private List<RoleItem> roles() {
        return entityManager.createQuery("""
                        SELECT COALESCE(r.nombreMostrar, r.nombre), COUNT(u)
                        FROM UsuarioEntity u
                        LEFT JOIN u.rol r
                        GROUP BY r.nombreMostrar, r.nombre
                        ORDER BY COUNT(u) DESC
                        """, Object[].class)
                .getResultList()
                .stream()
                .map(row -> new RoleItem(Objects.toString(row[0], "Sin rol"), ((Number) row[1]).longValue()))
                .toList();
    }

    private long scalarLong(String jpql) {
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    private long scalarLong(String jpql, String parameterName, Object parameterValue) {
        return entityManager.createQuery(jpql, Long.class)
                .setParameter(parameterName, parameterValue)
                .getSingleResult();
    }

    private String money(BigDecimal value) {
        return clpFormat.format(value != null ? value : BigDecimal.ZERO);
    }

    private long percent(long value, long total) {
        return total > 0 ? Math.round(value * 100.0 / total) : 0;
    }

    public record AdminDashboardResponse(
            List<MetricItem> metrics,
            List<DashboardDocumentItem> recentDocuments,
            List<DashboardAlertItem> alerts,
            List<FolioItem> folios,
            List<QuickActionItem> quickActions
    ) {}

    public record SuperAdminDashboardResponse(
            List<MetricItem> metrics,
            List<CompanyRankingItem> topCompanies,
            List<DashboardAlertItem> alerts,
            List<RoleItem> roles,
            List<QuickActionItem> quickActions
    ) {}

    public record MetricItem(String label, String value, String detail, String icon, String tone) {}
    public record DashboardDocumentItem(String id, String client, String date, String amount, String status) {}
    public record DashboardAlertItem(String title, String description, String icon, String tone) {}
    public record FolioItem(String name, int available, int total, double percent) {}
    public record QuickActionItem(String label, String icon, String route) {}
    public record CompanyRankingItem(String name, long documents, String total, double percent) {}
    public record RoleItem(String name, long count) {}
}
