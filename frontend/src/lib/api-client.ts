const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function throwApiError(response: Response): Promise<never> {
  let message = `API request failed: ${response.status} ${response.statusText}`;
  try {
    const body = (await response.clone().json()) as {
      detail?: string;
      message?: string;
      title?: string;
    };
    message = body.detail ?? body.message ?? body.title ?? message;
  } catch {
    // Keep the HTTP status message when the server does not return JSON.
  }
  throw new ApiError(message, response.status);
}

export const customInstance = async <T>(
  url: string,
  options?: RequestInit,
): Promise<T> => {
  const response = await fetch(new URL(url, apiBaseUrl), {
    ...options,
    credentials: "include",
  });

  if (!response.ok) {
    return throwApiError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
};

export async function customDownload(
  url: string,
  options?: RequestInit,
): Promise<Blob> {
  const response = await fetch(new URL(url, apiBaseUrl), {
    ...options,
    credentials: "include",
  });
  if (!response.ok) {
    return throwApiError(response);
  }
  return response.blob();
}
