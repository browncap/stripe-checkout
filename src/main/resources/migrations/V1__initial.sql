CREATE TABLE users (
  user_id UUID NOT NULL,
  stripe_customer_id TEXT NOT NULL,
  PRIMARY KEY (user_id),
  UNIQUE (stripe_customer_id)
);
