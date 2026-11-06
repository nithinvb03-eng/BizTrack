import { defineConfig } from "orval";

export default defineConfig({
  biztrack: {
    input: {
      target: "../openapi.yaml",
    },
    output: {
      mode: "tags-split",
      target: "src/generated",
      schemas: "src/generated/model",
      client: "react-query",
      override: {
        fetch: {
          includeHttpResponseReturnType: false,
        },
        mutator: {
          path: "src/lib/api-client.ts",
          name: "customInstance",
        },
      },
    },
  },
});
