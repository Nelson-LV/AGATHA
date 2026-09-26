## Language convention

All code in this project is written in English: class/interface/enum names, properties,
parameters, function names, and enum constants. This applies to every layer (domain, data,
ui, core) — see `docs/ARQUITECTURA_Y_DISENO.md` § "Convención de idioma" for the rationale
and the full before/after naming reference from the HE-04..HE-08 refactor.

Exceptions:
- **Comments and KDoc** may stay in Spanish (the team's working language). Do not translate
  existing Spanish comments as a side effect of unrelated changes.
- **User-facing strings** (anything a `Text()`, `contentDescription`, snackbar, etc. shows to
  the field technician) are the one place Spanish is the primary language, and they must
  eventually support **both Spanish and English** via Android string resources
  (`res/values/strings.xml` for Spanish as default + `res/values-en/strings.xml` for
  English) — not hardcoded literals in Composables. That resource-based bilingual migration
  has not been done yet (tracked as future work, not part of the HE-04..HE-08 vocabulary
  refactor): existing screens still have Spanish text hardcoded inline. When you touch a
  screen, prefer moving its literals into string resources over adding more hardcoded text.

When adding new code: name it in English from the start. When editing an existing Spanish
identifier you encounter, rename it to English as part of that change.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
