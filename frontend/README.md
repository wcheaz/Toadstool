# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.1.8.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

API requests use relative `/api` URLs. The development server forwards these to
`http://localhost:8081` using [proxy.conf.json](./proxy.conf.json). Start the local
backend separately and restart `ng serve` after changing proxy settings.

## Docker deployment

The [Dockerfile](./Dockerfile) builds the production application with Node 24 and
serves `dist/frontend/browser` using unprivileged Nginx on container port 8080.
The [Nginx configuration](./nginx.conf) supports Angular deep links and proxies
`/api` requests to `api:8080` on the Compose network, preserving paths, query
strings, and authorization headers. Docker DNS is re-resolved periodically so
backend container replacement does not require a frontend image rebuild.

No backend credentials or API keys are included in the browser image. See the
[deployment instructions](../README.md#frontend-container-on-the-existing-vm)
for VM commands and port configuration.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
