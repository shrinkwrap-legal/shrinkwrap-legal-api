-- Enable pgvector so Hibernate can create vector(1536) columns (CaseLawEmbeddingEntity).
CREATE EXTENSION IF NOT EXISTS vector;