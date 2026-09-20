package ma.clubify.platform.controller;

import ma.clubify.generated.api.ExportsApi;
import ma.clubify.generated.model.ExportRequest;
import ma.clubify.platform.service.ExportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Exports de listes en CSV ou en Excel. */
@RestController
public class ExportsController implements ExportsApi {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService exports;

    public ExportsController(ExportService exports) {
        this.exports = exports;
    }

    /**
     * La permission dépend de la liste demandée : elle est donc vérifiée par le
     * service, qui la connaît.
     */
    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<Resource> createExport(ExportRequest demande) {
        ExportService.Resultat resultat = exports.exporter(
                demande.getDataset(), demande.getFormat().getValue());

        boolean excel = "XLSX".equals(resultat.format());
        String nom = demande.getDataset() + (excel ? ".xlsx" : ".csv");

        return ResponseEntity.ok()
                .contentType(excel ? XLSX : MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
                .body(new ByteArrayResource(resultat.octets()));
    }
}
