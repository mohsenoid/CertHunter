## ADDED Requirements

### Requirement: Detail-dialog local UI state is package-scoped

Local UI state owned by the certificate detail dialog, such as whether the historical-certificates section is expanded, SHALL belong to the currently displayed package only. Opening a different app's detail dialog SHALL NOT restore local expansion state from a previous app.

#### Scenario: History expansion does not leak across apps

- **WHEN** the user opens app A's detail dialog and expands the historical certificates section
- **AND** dismisses that dialog
- **AND** later opens app B's detail dialog
- **THEN** app B's historical certificates section starts from its default collapsed state
- **AND** app A's previous expansion state does not leak into app B's dialog
