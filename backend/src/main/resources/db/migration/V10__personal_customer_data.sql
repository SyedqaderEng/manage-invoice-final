ALTER TABLE documents ADD COLUMN owner_user_id UUID REFERENCES users(id);
UPDATE documents d
SET owner_user_id = w.owner_user_id
FROM workspaces w
WHERE d.workspace_id = w.id AND d.owner_user_id IS NULL;
ALTER TABLE documents ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE documents ALTER COLUMN workspace_id DROP NOT NULL;
CREATE INDEX idx_documents_owner_uploaded ON documents(owner_user_id, uploaded_at DESC);
CREATE INDEX idx_documents_owner_checksum ON documents(owner_user_id, checksum);

ALTER TABLE vendors ADD COLUMN owner_user_id UUID REFERENCES users(id);
UPDATE vendors v
SET owner_user_id = w.owner_user_id
FROM workspaces w
WHERE v.workspace_id = w.id AND v.owner_user_id IS NULL;
ALTER TABLE vendors ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE vendors ALTER COLUMN workspace_id DROP NOT NULL;
CREATE INDEX idx_vendors_owner_name ON vendors(owner_user_id, normalized_name);

ALTER TABLE categories ADD COLUMN owner_user_id UUID REFERENCES users(id);
UPDATE categories c
SET owner_user_id = w.owner_user_id
FROM workspaces w
WHERE c.workspace_id = w.id AND c.owner_user_id IS NULL;
ALTER TABLE categories ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE categories ALTER COLUMN workspace_id DROP NOT NULL;
CREATE INDEX idx_categories_owner_name ON categories(owner_user_id, name);
