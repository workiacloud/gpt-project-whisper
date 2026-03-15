package cloud.workia.sync.controller;

import cloud.workia.sync.model.AutocompleteSuggestion;
import cloud.workia.sync.service.AutocompleteService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autocomplete")
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    @GetMapping("/{table}")
    public List<AutocompleteSuggestion> autocomplete(
            @PathVariable String table,
            @RequestParam String field,
            @RequestParam String term,
            @RequestParam(defaultValue = "10") int limit
    ) {

        return autocompleteService.autocomplete(table, field, term, limit);
    }
}