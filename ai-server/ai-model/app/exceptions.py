from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

try:
    from .schemas import ErrorResponse
except ImportError:
    from schemas import ErrorResponse


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(RequestValidationError)
    async def request_validation_handler(request: Request, exc: RequestValidationError):
        body = ErrorResponse(
            code="INVALID_REQUEST",
            message="Request validation failed",
            detail=str(exc),
        )
        return JSONResponse(status_code=422, content=body.model_dump())

    @app.exception_handler(HTTPException)
    async def http_exception_handler(request: Request, exc: HTTPException):
        detail = exc.detail if isinstance(exc.detail, str) else str(exc.detail)
        body = ErrorResponse(
            code="HTTP_ERROR",
            message="Request failed",
            detail=detail,
        )
        return JSONResponse(status_code=exc.status_code, content=body.model_dump())

    @app.exception_handler(Exception)
    async def unexpected_error_handler(request: Request, exc: Exception):
        body = ErrorResponse(
            code="INTERNAL_ERROR",
            message="Unexpected server error",
            detail=str(exc),
        )
        return JSONResponse(status_code=500, content=body.model_dump())
