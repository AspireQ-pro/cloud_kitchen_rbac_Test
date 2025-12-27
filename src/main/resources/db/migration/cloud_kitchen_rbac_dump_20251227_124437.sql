--
-- PostgreSQL database dump
--

\restrict KEapfE5DoifF1E60xRBXyHwHi0nSIJ7jaxYJHTlWphk3xalFDnSZRLezw843qU3

-- Dumped from database version 17.6
-- Dumped by pg_dump version 17.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: asset_type_enum; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.asset_type_enum AS ENUM (
    'LOGO',
    'BANNER',
    'PHOTO',
    'PRODUCT_IMAGE'
);


ALTER TYPE public.asset_type_enum OWNER TO postgres;

--
-- Name: check_management_images_limit(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.check_management_images_limit() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_active_count INTEGER;
BEGIN
    -- If the new row is not active, we don't care about the limit
    IF NEW.is_active IS DISTINCT FROM TRUE THEN
        RETURN NEW;
    END IF;

    -- Count existing active images
    IF TG_OP = 'INSERT' THEN
        SELECT COUNT(*)
        INTO v_active_count
        FROM management_images
        WHERE is_active = TRUE;
    ELSIF TG_OP = 'UPDATE' THEN
        SELECT COUNT(*)
        INTO v_active_count
        FROM management_images
        WHERE is_active = TRUE
          AND image_id <> NEW.image_id;
    END IF;

    -- Adding/keeping this row active would exceed limit of 3
    IF v_active_count >= 3 THEN
        RAISE EXCEPTION 'Cannot have more than 3 active management images';
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.check_management_images_limit() OWNER TO postgres;

--
-- Name: cleanup_expired_otps(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.cleanup_expired_otps() RETURNS integer
    LANGUAGE plpgsql
    AS $$ 
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM otp_logs WHERE expires_at < CURRENT_TIMESTAMP;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    UPDATE users SET 
        otp_code = NULL, 
        otp_expires_at = NULL,
        otp_attempts = 0
    WHERE otp_expires_at < CURRENT_TIMESTAMP;
    
    RETURN deleted_count;
END;
$$;


ALTER FUNCTION public.cleanup_expired_otps() OWNER TO postgres;

--
-- Name: compute_item_amounts(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.compute_item_amounts() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.subtotal := COALESCE(NEW.unit_price, 0) * COALESCE(NEW.quantity, 0);
    NEW.total_amount := NEW.subtotal + COALESCE(NEW.tax_amount, 0) - COALESCE(NEW.discount_amount, 0);
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.compute_item_amounts() OWNER TO postgres;

--
-- Name: generate_otp(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.generate_otp() RETURNS character varying
    LANGUAGE plpgsql
    AS $$ 
BEGIN
    RETURN LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
END;
$$;


ALTER FUNCTION public.generate_otp() OWNER TO postgres;

--
-- Name: is_phone_blocked(character varying, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.is_phone_blocked(p_phone character varying, p_merchant_id integer) RETURNS boolean
    LANGUAGE plpgsql
    AS $$ 
DECLARE
    blocked_until TIMESTAMP;
BEGIN
    SELECT otp_blocked_until INTO blocked_until 
    FROM users 
    WHERE phone = p_phone AND merchant_id = p_merchant_id;
    
    RETURN blocked_until IS NOT NULL AND blocked_until > CURRENT_TIMESTAMP;
END;
$$;


ALTER FUNCTION public.is_phone_blocked(p_phone character varying, p_merchant_id integer) OWNER TO postgres;

--
-- Name: log_order_status_change(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.log_order_status_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.order_status <> OLD.order_status THEN

        INSERT INTO order_status_history (
            order_id, from_status, to_status, changed_by_role
        ) VALUES (
            NEW.order_id, OLD.order_status, NEW.order_status, 'system'
        );

        IF OLD.order_status = 'NEW' AND NEW.confirmed_at IS NULL THEN
            NEW.confirmed_at = CURRENT_TIMESTAMP;
        END IF;

        IF NEW.order_status = 'PREPARING' THEN
            NEW.preparing_at = CURRENT_TIMESTAMP;
        ELSIF NEW.order_status = 'OUT_FOR_DELIVERY' THEN
            NEW.dispatched_at = CURRENT_TIMESTAMP;
        ELSIF NEW.order_status = 'DELIVERED' THEN
            NEW.delivered_at = CURRENT_TIMESTAMP;
        ELSIF NEW.order_status = 'CANCELLED' THEN
            NEW.cancelled_at = CURRENT_TIMESTAMP;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.log_order_status_change() OWNER TO postgres;

--
-- Name: notify_order_items_change(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.notify_order_items_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    payload TEXT;
BEGIN
    IF (TG_OP = 'INSERT') THEN
        payload := 'INSERT:' || NEW.order_id::TEXT;
    ELSIF (TG_OP = 'UPDATE') THEN
        payload := 'UPDATE:' || NEW.order_id::TEXT;
    ELSIF (TG_OP = 'DELETE') THEN
        payload := 'DELETE:' || OLD.order_id::TEXT;
    ELSE
        payload := TG_OP || ':';
    END IF;

    PERFORM pg_notify('order_items_changes', payload);
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;


ALTER FUNCTION public.notify_order_items_change() OWNER TO postgres;

--
-- Name: update_ads_updated_at(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_ads_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_ads_updated_at() OWNER TO postgres;

--
-- Name: update_offer_timestamp(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_offer_timestamp() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_offer_timestamp() OWNER TO postgres;

--
-- Name: update_order_totals(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_order_totals() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    oid INTEGER;
BEGIN
    oid := COALESCE(NEW.order_id, OLD.order_id);

    UPDATE orders
    SET
        subtotal = COALESCE((SELECT SUM(subtotal) FROM order_items WHERE order_id = oid), 0),
        tax_amount = COALESCE((SELECT SUM(tax_amount) FROM order_items WHERE order_id = oid), 0),
        discount_amount = COALESCE((SELECT SUM(discount_amount) FROM order_items WHERE order_id = oid), 0),
        total_amount =
            COALESCE(
                (SELECT SUM(subtotal + tax_amount - discount_amount)
                 FROM order_items WHERE order_id = oid),
                0
            ) + delivery_fee,
        updated_at = CURRENT_TIMESTAMP
    WHERE order_id = oid;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_order_totals() OWNER TO postgres;

--
-- Name: update_timestamp(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_timestamp() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
   NEW.updated_on = CURRENT_TIMESTAMP;
   RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_timestamp() OWNER TO postgres;

--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO postgres;

--
-- Name: update_updated_on_column(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_updated_on_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_on = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_on_column() OWNER TO postgres;

--
-- Name: update_website_config_updated_at(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_website_config_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_website_config_updated_at() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ads; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ads (
    ad_id integer NOT NULL,
    ad_number character varying(5) NOT NULL,
    ad_name character varying(255) NOT NULL,
    ad_code character varying(100) NOT NULL,
    from_date date NOT NULL,
    to_date date NOT NULL,
    image_url text NOT NULL,
    display_status boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by integer NOT NULL,
    updated_by integer,
    image_s3_key character varying(500),
    CONSTRAINT valid_ad_code_length CHECK ((length((ad_code)::text) >= 3)),
    CONSTRAINT valid_ad_name_length CHECK ((length((ad_name)::text) >= 2)),
    CONSTRAINT valid_ad_number CHECK (((ad_number)::text ~ '^[0-9]{5}$'::text)),
    CONSTRAINT valid_date_range CHECK ((to_date > from_date))
);


ALTER TABLE public.ads OWNER TO postgres;

--
-- Name: TABLE ads; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.ads IS 'Stores advertisement information';


--
-- Name: COLUMN ads.ad_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.ads.ad_number IS 'Unique 5-digit advertisement number';


--
-- Name: COLUMN ads.ad_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.ads.ad_code IS 'Unique advertisement code for identification';


--
-- Name: COLUMN ads.display_status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.ads.display_status IS 'Whether the ad should be displayed';


--
-- Name: ads_ad_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ads_ad_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.ads_ad_id_seq OWNER TO postgres;

--
-- Name: ads_ad_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ads_ad_id_seq OWNED BY public.ads.ad_id;


--
-- Name: customers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.customers (
    customer_id integer NOT NULL,
    merchant_id integer NOT NULL,
    user_id integer,
    phone character varying(20) NOT NULL,
    email character varying(255),
    first_name character varying(100),
    last_name character varying(100),
    address text,
    city character varying(100),
    state character varying(100),
    country character varying(100) DEFAULT 'India'::character varying,
    pincode character varying(10),
    is_active boolean DEFAULT true,
    deleted_at timestamp without time zone,
    created_by integer,
    updated_by integer,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    dob date,
    favorite_food character varying(255),
    last_order_at timestamp(6) without time zone,
    total_orders integer,
    profile_image_url character varying(500),
    CONSTRAINT chk_customer_phone_not_empty CHECK (((phone)::text <> ''::text))
);


ALTER TABLE public.customers OWNER TO postgres;

--
-- Name: customers_customer_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.customers_customer_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.customers_customer_id_seq OWNER TO postgres;

--
-- Name: customers_customer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.customers_customer_id_seq OWNED BY public.customers.customer_id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: image_records; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.image_records (
    id integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    entity_id character varying(255),
    image_type character varying(50) NOT NULL,
    is_active boolean NOT NULL,
    merchant_id integer NOT NULL,
    s3_key character varying(255) NOT NULL,
    s3_url character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);


ALTER TABLE public.image_records OWNER TO postgres;

--
-- Name: image_records_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.image_records_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.image_records_id_seq OWNER TO postgres;

--
-- Name: image_records_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.image_records_id_seq OWNED BY public.image_records.id;


--
-- Name: menu_cards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.menu_cards (
    menu_card_id bigint NOT NULL,
    merchant_id integer NOT NULL,
    menu_card_name character varying(255),
    menu_card_image_s3_key character varying(500),
    menu_card_image_url character varying(500),
    is_active boolean DEFAULT true,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.menu_cards OWNER TO postgres;

--
-- Name: menu_cards_menu_card_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.menu_cards_menu_card_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.menu_cards_menu_card_id_seq OWNER TO postgres;

--
-- Name: menu_cards_menu_card_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.menu_cards_menu_card_id_seq OWNED BY public.menu_cards.menu_card_id;


--
-- Name: merchant_products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.merchant_products (
    product_id integer NOT NULL,
    merchant_id integer NOT NULL,
    product_name character varying(255) NOT NULL,
    category character varying(100) NOT NULL,
    description text,
    price numeric(10,2) NOT NULL,
    stock_quantity integer DEFAULT 0,
    weight_kg numeric(8,2),
    image_url character varying(500),
    is_available boolean DEFAULT true,
    created_by integer,
    updated_by integer,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    status character varying(20) DEFAULT 'draft'::character varying,
    is_display boolean DEFAULT true NOT NULL
);


ALTER TABLE public.merchant_products OWNER TO postgres;

--
-- Name: COLUMN merchant_products.is_display; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.merchant_products.is_display IS 'Whether product should be displayed to customers';


--
-- Name: merchant_products_product_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.merchant_products_product_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.merchant_products_product_id_seq OWNER TO postgres;

--
-- Name: merchant_products_product_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.merchant_products_product_id_seq OWNED BY public.merchant_products.product_id;


--
-- Name: merchant_website_assets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.merchant_website_assets (
    id bigint NOT NULL,
    merchant_id integer NOT NULL,
    asset_type public.asset_type_enum NOT NULL,
    original_filename character varying(255) NOT NULL,
    s3_key character varying(1000) NOT NULL,
    s3_url character varying(1000) NOT NULL,
    file_size_bytes bigint,
    mime_type character varying(100),
    width_px integer,
    height_px integer,
    uploaded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.merchant_website_assets OWNER TO postgres;

--
-- Name: TABLE merchant_website_assets; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.merchant_website_assets IS 'Tracks uploaded assets for merchant websites';


--
-- Name: merchant_website_assets_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.merchant_website_assets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.merchant_website_assets_id_seq OWNER TO postgres;

--
-- Name: merchant_website_assets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.merchant_website_assets_id_seq OWNED BY public.merchant_website_assets.id;


--
-- Name: merchant_website_config; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.merchant_website_config (
    id bigint NOT NULL,
    merchant_id integer NOT NULL,
    kitchen_name character varying(255) NOT NULL,
    subdomain character varying(100) NOT NULL,
    website_address character varying(500) NOT NULL,
    address text,
    description text,
    logo_url character varying(1000),
    banner_url character varying(1000),
    photo_url character varying(1000),
    whatsapp character varying(50),
    instagram character varying(255),
    youtube character varying(255),
    twitter character varying(255),
    facebook character varying(255),
    is_published boolean DEFAULT false,
    website_s3_path character varying(1000),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    published_at timestamp without time zone,
    CONSTRAINT chk_subdomain_format CHECK ((((subdomain)::text ~ '^[a-z0-9][a-z0-9-]*[a-z0-9]$'::text) AND (length((subdomain)::text) >= 3) AND (length((subdomain)::text) <= 63)))
);


ALTER TABLE public.merchant_website_config OWNER TO postgres;

--
-- Name: TABLE merchant_website_config; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.merchant_website_config IS 'Stores website configuration for each merchant';


--
-- Name: COLUMN merchant_website_config.website_address; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.merchant_website_config.website_address IS 'Generated website URL for the merchant';


--
-- Name: COLUMN merchant_website_config.is_published; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.merchant_website_config.is_published IS 'Whether the website is currently published and accessible';


--
-- Name: merchant_website_config_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.merchant_website_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.merchant_website_config_id_seq OWNER TO postgres;

--
-- Name: merchant_website_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.merchant_website_config_id_seq OWNED BY public.merchant_website_config.id;


--
-- Name: merchant_website_theme; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.merchant_website_theme (
    id bigint NOT NULL,
    merchant_id integer NOT NULL,
    primary_color character varying(7) DEFAULT '#FBCE1A'::character varying,
    secondary_color character varying(7) DEFAULT '#2E2E2E'::character varying,
    font_family character varying(100) DEFAULT 'Figtree'::character varying,
    custom_css text,
    template_name character varying(50) DEFAULT 'default'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.merchant_website_theme OWNER TO postgres;

--
-- Name: merchant_website_theme_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.merchant_website_theme_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.merchant_website_theme_id_seq OWNER TO postgres;

--
-- Name: merchant_website_theme_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.merchant_website_theme_id_seq OWNED BY public.merchant_website_theme.id;


--
-- Name: merchants; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.merchants (
    merchant_id integer NOT NULL,
    merchant_name character varying(255) NOT NULL,
    business_name character varying(255) NOT NULL,
    business_type character varying(100) DEFAULT 'restaurant'::character varying,
    website_url character varying(500),
    phone character varying(20),
    email character varying(255),
    address text,
    city character varying(100),
    state character varying(100),
    country character varying(100) DEFAULT 'India'::character varying,
    pincode character varying(10),
    gstin character varying(20),
    fssai_license character varying(50),
    is_active boolean DEFAULT true,
    subscription_plan character varying(50) DEFAULT 'basic'::character varying,
    subscription_expires_at timestamp without time zone,
    created_by integer,
    updated_by integer,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.merchants OWNER TO postgres;

--
-- Name: merchants_merchant_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.merchants_merchant_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.merchants_merchant_id_seq OWNER TO postgres;

--
-- Name: merchants_merchant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.merchants_merchant_id_seq OWNED BY public.merchants.merchant_id;


--
-- Name: offers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.offers (
    offer_id integer NOT NULL,
    merchant_id integer,
    offer_name character varying(255) NOT NULL,
    offer_number character varying(50) NOT NULL,
    offer_code character varying(100) NOT NULL,
    buy_quantity integer,
    get_quantity integer,
    offer_image_s3_key character varying(500),
    offer_image_url character varying(500),
    start_date date NOT NULL,
    end_date date NOT NULL,
    description text,
    terms_and_conditions text,
    discount_type character varying(30) DEFAULT 'percentage'::character varying,
    discount_value numeric(10,2),
    max_discount_amount numeric(10,2),
    min_purchase_amount numeric(10,2),
    max_uses_per_customer integer DEFAULT 1,
    total_usage_limit integer,
    current_usage_count integer DEFAULT 0,
    status character varying(20) DEFAULT 'draft'::character varying,
    is_active boolean DEFAULT true,
    is_featured boolean DEFAULT false,
    is_display boolean DEFAULT true,
    offer_scope character varying(20) GENERATED ALWAYS AS (
CASE
    WHEN (merchant_id IS NULL) THEN 'global'::text
    ELSE 'merchant'::text
END) STORED,
    created_by_merchant_id integer,
    updated_by_merchant_id integer,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted_on timestamp without time zone,
    CONSTRAINT offers_discount_type_check CHECK (((discount_type)::text = ANY ((ARRAY['percentage'::character varying, 'fixed_amount'::character varying, 'buy_x_get_y'::character varying, 'free_shipping'::character varying])::text[]))),
    CONSTRAINT offers_status_check CHECK (((status)::text = ANY ((ARRAY['draft'::character varying, 'active'::character varying, 'paused'::character varying, 'expired'::character varying, 'cancelled'::character varying])::text[])))
);


ALTER TABLE public.offers OWNER TO postgres;

--
-- Name: offers_offer_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.offers_offer_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.offers_offer_id_seq OWNER TO postgres;

--
-- Name: offers_offer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.offers_offer_id_seq OWNED BY public.offers.offer_id;


--
-- Name: order_filters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_filters (
    filter_id integer NOT NULL,
    merchant_id integer NOT NULL,
    filter_name character varying(255),
    from_date date,
    to_date date,
    selected_day character varying(20),
    from_time time without time zone,
    to_time time without time zone,
    status_filters character varying(50)[] DEFAULT ARRAY['NEW'::text, 'PREPARING'::text, 'OUT_FOR_DELIVERY'::text, 'DELIVERED'::text, 'CANCELLED'::text],
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT order_filters_selected_day_check CHECK (((selected_day IS NULL) OR ((selected_day)::text = ANY ((ARRAY['Sunday'::character varying, 'Monday'::character varying, 'Tuesday'::character varying, 'Wednesday'::character varying, 'Thursday'::character varying, 'Friday'::character varying, 'Saturday'::character varying])::text[]))))
);


ALTER TABLE public.order_filters OWNER TO postgres;

--
-- Name: order_filters_filter_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.order_filters_filter_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.order_filters_filter_id_seq OWNER TO postgres;

--
-- Name: order_filters_filter_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.order_filters_filter_id_seq OWNED BY public.order_filters.filter_id;


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_items (
    item_id integer NOT NULL,
    order_id integer NOT NULL,
    product_id integer NOT NULL,
    product_name character varying(255) NOT NULL,
    product_sku character varying(100),
    category_name character varying(255),
    unit_price numeric(12,2) NOT NULL,
    quantity integer NOT NULL,
    subtotal numeric(12,2) DEFAULT 0.00 NOT NULL,
    tax_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    discount_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    total_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    special_instructions text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT order_items_quantity_check1 CHECK ((quantity > 0))
);


ALTER TABLE public.order_items OWNER TO postgres;

--
-- Name: order_items_item_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.order_items_item_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.order_items_item_id_seq OWNER TO postgres;

--
-- Name: order_items_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.order_items_item_id_seq OWNED BY public.order_items.item_id;


--
-- Name: order_items_staging; Type: TABLE; Schema: public; Owner: postgres
--

CREATE UNLOGGED TABLE public.order_items_staging (
    item_id integer NOT NULL,
    order_id integer NOT NULL,
    product_id integer NOT NULL,
    product_name character varying(255) NOT NULL,
    product_sku character varying(100),
    category_name character varying(255),
    unit_price numeric(12,2) DEFAULT 0.00 NOT NULL,
    quantity integer NOT NULL,
    subtotal numeric(12,2) DEFAULT 0.00 NOT NULL,
    tax_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    discount_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    total_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    special_instructions text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT order_items_quantity_check CHECK ((quantity > 0))
);


ALTER TABLE public.order_items_staging OWNER TO postgres;

--
-- Name: order_status_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_status_history (
    history_id integer NOT NULL,
    order_id integer NOT NULL,
    from_status character varying(50),
    to_status character varying(50) NOT NULL,
    changed_by integer,
    changed_by_role character varying(50),
    notes text,
    changed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT order_status_history_changed_by_role_check CHECK (((changed_by_role)::text = ANY ((ARRAY['merchant'::character varying, 'admin'::character varying, 'system'::character varying, 'customer'::character varying])::text[])))
);


ALTER TABLE public.order_status_history OWNER TO postgres;

--
-- Name: order_status_history_history_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.order_status_history_history_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.order_status_history_history_id_seq OWNER TO postgres;

--
-- Name: order_status_history_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.order_status_history_history_id_seq OWNED BY public.order_status_history.history_id;


--
-- Name: orders; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.orders (
    order_id integer NOT NULL,
    merchant_id integer NOT NULL,
    customer_id integer NOT NULL,
    customer_name character varying(255) NOT NULL,
    customer_mobile character varying(20) NOT NULL,
    customer_email character varying(255),
    delivery_address text NOT NULL,
    delivery_city character varying(100),
    delivery_pincode character varying(10),
    delivery_latitude numeric(10,8),
    delivery_longitude numeric(11,8),
    order_status character varying(50) DEFAULT 'NEW'::character varying NOT NULL,
    placed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    confirmed_at timestamp without time zone,
    preparing_at timestamp without time zone,
    dispatched_at timestamp without time zone,
    delivered_at timestamp without time zone,
    cancelled_at timestamp without time zone,
    subtotal numeric(12,2) DEFAULT 0.00 NOT NULL,
    tax_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    delivery_fee numeric(12,2) DEFAULT 0.00 NOT NULL,
    discount_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    total_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    payment_method character varying(50),
    payment_status character varying(50) DEFAULT 'PENDING'::character varying NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_total_non_negative CHECK ((total_amount >= (0)::numeric)),
    CONSTRAINT orders_order_status_check CHECK (((order_status)::text = ANY ((ARRAY['NEW'::character varying, 'PREPARING'::character varying, 'OUT_FOR_DELIVERY'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT orders_payment_method_check CHECK (((payment_method IS NULL) OR ((payment_method)::text = ANY ((ARRAY['COD'::character varying, 'UPI'::character varying, 'CARD'::character varying, 'WALLET'::character varying, 'NETBANKING'::character varying])::text[])))),
    CONSTRAINT orders_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[])))
);


ALTER TABLE public.orders OWNER TO postgres;

--
-- Name: orders_order_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.orders_order_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.orders_order_id_seq OWNER TO postgres;

--
-- Name: orders_order_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.orders_order_id_seq OWNED BY public.orders.order_id;


--
-- Name: otp_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.otp_logs (
    otp_log_id integer NOT NULL,
    merchant_id integer,
    phone character varying(20) NOT NULL,
    otp_code character varying(4) NOT NULL,
    otp_type character varying(20) DEFAULT 'login'::character varying,
    status character varying(20) DEFAULT 'sent'::character varying,
    ip_address character varying(45),
    user_agent text,
    attempts_count integer DEFAULT 0,
    verified_at timestamp without time zone,
    expires_at timestamp without time zone NOT NULL,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT otp_logs_otp_type_check CHECK (((otp_type)::text = ANY ((ARRAY['login'::character varying, 'registration'::character varying, 'password_reset'::character varying, 'phone_verification'::character varying])::text[]))),
    CONSTRAINT otp_logs_status_check CHECK (((status)::text = ANY ((ARRAY['sent'::character varying, 'verified'::character varying, 'expired'::character varying, 'failed'::character varying])::text[])))
);


ALTER TABLE public.otp_logs OWNER TO postgres;

--
-- Name: otp_logs_otp_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.otp_logs_otp_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.otp_logs_otp_log_id_seq OWNER TO postgres;

--
-- Name: otp_logs_otp_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.otp_logs_otp_log_id_seq OWNED BY public.otp_logs.otp_log_id;


--
-- Name: permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.permissions (
    permission_id integer NOT NULL,
    permission_name character varying(100) NOT NULL,
    resource character varying(100) NOT NULL,
    action character varying(50) NOT NULL,
    description text,
    created_by integer,
    updated_by integer,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.permissions OWNER TO postgres;

--
-- Name: permissions_permission_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.permissions_permission_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.permissions_permission_id_seq OWNER TO postgres;

--
-- Name: permissions_permission_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.permissions_permission_id_seq OWNED BY public.permissions.permission_id;


--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.role_permissions (
    role_permission_id integer NOT NULL,
    role_id integer NOT NULL,
    permission_id integer NOT NULL,
    created_by integer,
    updated_by integer,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.role_permissions OWNER TO postgres;

--
-- Name: role_permissions_role_permission_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.role_permissions_role_permission_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.role_permissions_role_permission_id_seq OWNER TO postgres;

--
-- Name: role_permissions_role_permission_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.role_permissions_role_permission_id_seq OWNED BY public.role_permissions.role_permission_id;


--
-- Name: roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.roles (
    role_id integer NOT NULL,
    role_name character varying(100) NOT NULL,
    description text,
    is_system_role boolean DEFAULT false,
    created_by integer,
    updated_by integer,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.roles OWNER TO postgres;

--
-- Name: roles_role_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.roles_role_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.roles_role_id_seq OWNER TO postgres;

--
-- Name: roles_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.roles_role_id_seq OWNED BY public.roles.role_id;


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    user_role_id integer NOT NULL,
    user_id integer NOT NULL,
    role_id integer NOT NULL,
    merchant_id integer,
    assigned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamp without time zone,
    created_by integer,
    updated_by integer,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.user_roles OWNER TO postgres;

--
-- Name: user_roles_user_role_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_roles_user_role_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_roles_user_role_id_seq OWNER TO postgres;

--
-- Name: user_roles_user_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_roles_user_role_id_seq OWNED BY public.user_roles.user_role_id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    user_id integer NOT NULL,
    merchant_id integer,
    phone character varying(20) NOT NULL,
    email character varying(255),
    username character varying(100),
    password_hash character varying(255),
    first_name character varying(100),
    last_name character varying(100),
    date_of_birth date,
    gender character varying(10),
    profile_image_url character varying(500),
    user_type character varying(20) DEFAULT 'customer'::character varying,
    is_active boolean DEFAULT true,
    is_verified boolean DEFAULT false,
    phone_verified boolean DEFAULT false,
    email_verified boolean DEFAULT false,
    last_login_at timestamp without time zone,
    password_reset_token character varying(255),
    password_reset_expires_at timestamp without time zone,
    email_verification_token character varying(255),
    email_verified_at timestamp without time zone,
    otp_code character varying(4),
    otp_expires_at timestamp without time zone,
    otp_attempts integer DEFAULT 0,
    otp_blocked_until timestamp without time zone,
    is_guest boolean DEFAULT false,
    guest_converted_at timestamp without time zone,
    preferred_login_method character varying(20) DEFAULT 'otp'::character varying,
    created_by integer,
    updated_by integer,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    address character varying(250),
    dietary_preferences character varying(200),
    favorite_food character varying(100),
    otp_used boolean,
    CONSTRAINT chk_guest_merchant CHECK (((is_guest = false) OR ((is_guest = true) AND (merchant_id IS NOT NULL)))),
    CONSTRAINT chk_guest_user_type CHECK (((is_guest = false) OR ((is_guest = true) AND ((user_type)::text = 'customer'::text)))),
    CONSTRAINT users_gender_check CHECK (((gender)::text = ANY ((ARRAY['male'::character varying, 'female'::character varying, 'other'::character varying])::text[]))),
    CONSTRAINT users_preferred_login_method_check CHECK (((preferred_login_method)::text = ANY ((ARRAY['password'::character varying, 'otp'::character varying, 'both'::character varying])::text[]))),
    CONSTRAINT users_user_type_check CHECK (((user_type)::text = ANY ((ARRAY['super_admin'::character varying, 'merchant'::character varying, 'customer'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_user_id_seq OWNER TO postgres;

--
-- Name: users_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_user_id_seq OWNED BY public.users.user_id;


--
-- Name: ads ad_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ads ALTER COLUMN ad_id SET DEFAULT nextval('public.ads_ad_id_seq'::regclass);


--
-- Name: customers customer_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers ALTER COLUMN customer_id SET DEFAULT nextval('public.customers_customer_id_seq'::regclass);


--
-- Name: image_records id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.image_records ALTER COLUMN id SET DEFAULT nextval('public.image_records_id_seq'::regclass);


--
-- Name: menu_cards menu_card_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.menu_cards ALTER COLUMN menu_card_id SET DEFAULT nextval('public.menu_cards_menu_card_id_seq'::regclass);


--
-- Name: merchant_products product_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_products ALTER COLUMN product_id SET DEFAULT nextval('public.merchant_products_product_id_seq'::regclass);


--
-- Name: merchant_website_assets id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_website_assets ALTER COLUMN id SET DEFAULT nextval('public.merchant_website_assets_id_seq'::regclass);


--
-- Name: merchant_website_config id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_website_config ALTER COLUMN id SET DEFAULT nextval('public.merchant_website_config_id_seq'::regclass);


--
-- Name: merchant_website_theme id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_website_theme ALTER COLUMN id SET DEFAULT nextval('public.merchant_website_theme_id_seq'::regclass);


--
-- Name: merchants merchant_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchants ALTER COLUMN merchant_id SET DEFAULT nextval('public.merchants_merchant_id_seq'::regclass);


--
-- Name: offers offer_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offers ALTER COLUMN offer_id SET DEFAULT nextval('public.offers_offer_id_seq'::regclass);


--
-- Name: order_filters filter_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_filters ALTER COLUMN filter_id SET DEFAULT nextval('public.order_filters_filter_id_seq'::regclass);


--
-- Name: order_items item_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items ALTER COLUMN item_id SET DEFAULT nextval('public.order_items_item_id_seq'::regclass);


--
-- Name: order_status_history history_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_status_history ALTER COLUMN history_id SET DEFAULT nextval('public.order_status_history_history_id_seq'::regclass);


--
-- Name: orders order_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders ALTER COLUMN order_id SET DEFAULT nextval('public.orders_order_id_seq'::regclass);


--
-- Name: otp_logs otp_log_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.otp_logs ALTER COLUMN otp_log_id SET DEFAULT nextval('public.otp_logs_otp_log_id_seq'::regclass);


--
-- Name: permissions permission_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permissions ALTER COLUMN permission_id SET DEFAULT nextval('public.permissions_permission_id_seq'::regclass);


--
-- Name: role_permissions role_permission_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions ALTER COLUMN role_permission_id SET DEFAULT nextval('public.role_permissions_role_permission_id_seq'::regclass);


--
-- Name: roles role_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles ALTER COLUMN role_id SET DEFAULT nextval('public.roles_role_id_seq'::regclass);


--
-- Name: user_roles user_role_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles ALTER COLUMN user_role_id SET DEFAULT nextval('public.user_roles_user_role_id_seq'::regclass);


--
-- Name: users user_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN user_id SET DEFAULT nextval('public.users_user_id_seq'::regclass);


--
-- Data for Name: ads; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ads (ad_id, ad_number, ad_name, ad_code, from_date, to_date, image_url, display_status, created_at, updated_at, created_by, updated_by, image_s3_key) FROM stdin;
2	60932	compani	FORU20	2025-11-28	2025-11-30	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/PANNER IMAGE.png	t	2025-11-28 11:23:58.44485	2025-11-28 11:23:58.44485	1	\N	ads/PANNER IMAGE.png
4	13274	firstads	FIRSTADD2	2025-12-04	2025-12-05	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1764761878160_575b2301.png	t	2025-12-03 11:37:59.157471	2025-12-03 11:37:59.157471	1	\N	0/ads/1764761878160_575b2301.png
5	89617	firstads	FIRSTADD21	2025-12-04	2025-12-05	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1764761926499_a6ef4bb5.png	t	2025-12-03 11:38:47.4639	2025-12-03 11:38:47.4639	1	\N	0/ads/1764761926499_a6ef4bb5.png
6	95693	firstadd	TEST123	2025-12-04	2025-12-31	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1764825906585_d5dd064b.jpg	t	2025-12-04 05:25:09.71996	2025-12-04 05:25:09.71996	1	\N	0/ads/1764825906585_d5dd064b.jpg
7	34481	Diwali Festival Sale 2025	DIWALI2025	2025-12-04	2025-12-30	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1764826658213_048435b7.png	t	2025-12-04 05:37:39.191656	2025-12-04 05:37:39.191656	1	\N	0/ads/1764826658213_048435b7.png
8	59009	Diwali Festival Sale 2025	DIWALI2026	2025-12-04	2025-12-30	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1764826923416_c4c028a0.png	t	2025-12-04 05:42:04.656698	2025-12-04 05:42:04.656698	1	\N	0/ads/1764826923416_c4c028a0.png
9	51479	firstadd	FIRST12	2025-12-04	2025-12-05	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1764827256053_3c5b2759.webp	t	2025-12-04 05:47:37.561439	2025-12-04 05:47:37.561439	1	\N	0/ads/1764827256053_3c5b2759.webp
10	47844	firstadd	FIRST11	2025-12-04	2025-12-05	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1764827803860_2ab4af85.webp	t	2025-12-04 05:56:46.237051	2025-12-04 05:56:46.237051	1	\N	0/ads/1764827803860_2ab4af85.webp
11	41030	Diwali Festival Sale 2025	DIWALI2027	2025-12-04	2025-12-30	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1764829014507_0f94526e.png	t	2025-12-04 06:16:55.540789	2025-12-04 06:16:55.540789	1	\N	0/ads/1764829014507_0f94526e.png
14	72585	globaladd	GOLBALADD	2026-01-01	2026-01-03	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1766135339395_e1bc777d.png	t	2025-12-19 09:09:04.38147	2025-12-19 09:09:04.38147	1	\N	0/ads/1766135339395_e1bc777d.png
15	29175	globaladd	ASLADD	2026-01-01	2026-01-03	https://aspireq-cloud-kitchen.s3.amazonaws.com/0/ads/1766136002054_304d008d.png	t	2025-12-19 09:20:04.496612	2025-12-19 09:20:04.496612	1	\N	0/ads/1766136002054_304d008d.png
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.customers (customer_id, merchant_id, user_id, phone, email, first_name, last_name, address, city, state, country, pincode, is_active, deleted_at, created_by, updated_by, created_on, updated_on, dob, favorite_food, last_order_at, total_orders, profile_image_url) FROM stdin;
2	1	14	9075027104	yogesh@email.com	Yogesh	Doe	123 Main Street	Mumbai	Maharashtra	India	400001	t	\N	14	\N	2025-12-22 14:13:19.131878	2025-12-22 14:13:19.131878	\N	\N	\N	\N	\N
3	1	15	9075027204	\N	John	Doe	123 Main Street	\N	\N	India	\N	t	\N	15	\N	2025-12-22 16:04:39.067783	2025-12-22 16:04:39.067783	\N	\N	\N	\N	\N
1	1	10	9075027004	john@example.com	Yogesh	Nagre	123 Main Street	Mumbai	Maharashtra	India	400001	t	\N	10	10	2025-12-18 16:28:52.793859	2025-12-24 17:46:05.535346	\N	\N	\N	\N	https://aspireq-cloud-kitchen.s3.amazonaws.com/1/customer/1/profile_img/profile.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251224T121605Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIASK5MCJJPRM2QFUVY%2F20251224%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=58a9e2dc17e52516b26459053cdfff4435276e5a507ee7bbbeb96a1b34e4e927
4	3	33	9075027004	\N	Yoges	Nagre	123 Main Street	\N	\N	India	\N	t	\N	33	\N	2025-12-27 12:38:05.944439	2025-12-27 12:38:05.944439	\N	\N	\N	\N	\N
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	0	<< Flyway Baseline >>	BASELINE	<< Flyway Baseline >>	\N	postgres	2025-11-21 18:02:29.021512	0	t
3	1.4	Add Display Columns	SQL	V1_4__Add_Display_Columns.sql	-137116688	postgres	2025-12-08 17:59:12.52514	132	t
4	1.5	Fix Display Columns	SQL	V1_5__Fix_Display_Columns.sql	1807660959	postgres	2025-12-08 17:59:12.709022	54	t
5	2	Create menu cards table	SQL	V2__Create_menu_cards_table.sql	1064981163	postgres	2025-12-08 17:59:12.788906	98	t
6	3	Create website tables	SQL	V3__Create_website_tables.sql	-539209606	postgres	2025-12-08 17:59:12.911172	64	t
7	4	Add subdomain column	SQL	V4__Add_subdomain_column.sql	-435249019	postgres	2025-12-08 17:59:13.041105	123	t
2	1	Create offers table	SQL	V1__initial_schema.sql	1736092591	postgres	2025-12-01 11:05:02.030174	11	t
8	5	Create ads table	SQL	V5__Create_ads_table.sql	-1344828986	postgres	2025-12-08 18:47:24.691832	115	t
9	6	Add Performance Indexes	SQL	V6__Add_Performance_Indexes.sql	351294585	postgres	2025-12-09 12:39:31.421799	127	t
\.


--
-- Data for Name: image_records; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.image_records (id, created_at, entity_id, image_type, is_active, merchant_id, s3_key, s3_url, updated_at) FROM stdin;
\.


--
-- Data for Name: menu_cards; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.menu_cards (menu_card_id, merchant_id, menu_card_name, menu_card_image_s3_key, menu_card_image_url, is_active, created_on, updated_on) FROM stdin;
1	1	\N	uploaded-s3-key	uploaded-image-url	t	2025-11-04 15:41:11.404686	2025-11-04 15:41:11.406188
3	3	\N	3/menu_card/1762341791673_9cb522f8.jpg	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/menu_card/1762341791673_9cb522f8.jpg	f	2025-11-05 16:53:13.953678	2025-11-05 16:59:30.224151
4	3	\N	3/menu_card/1762342506951_f8cf55a6.jpg	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/menu_card/1762342506951_f8cf55a6.jpg	t	2025-11-05 16:59:30.225895	2025-11-05 17:05:08.2193
5	3	\N	3/menu_card/1762342752033_e4c0bd7c.jpg	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/menu_card/1762342752033_e4c0bd7c.jpg	t	2025-11-05 17:09:17.328037	2025-11-05 17:09:17.328037
6	3	\N	3/menu_card/1762410609846_b97769d5.jpg	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/menu_card/1762410609846_b97769d5.jpg	t	2025-11-06 12:00:12.655306	2025-11-06 12:00:12.655306
7	1	\N	1/menu_card/1763830099677_fe9b5cca.jpg	https://aspireq-cloud-kitchen.s3.amazonaws.com/1/menu_card/1763830099677_fe9b5cca.jpg	t	2025-11-22 22:18:22.682314	2025-11-22 22:18:22.682314
2	3	\N	3/menucard_image/menucard_2_1764584442526_d9f4fe.jpg	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/menucard_image/menucard_2_1764584442526_d9f4fe.jpg	f	2025-11-05 16:48:31.911197	2025-12-01 15:50:43.652767
8	3	\N	3/menucard_image/menucard_1764678284407_aea8da.jpg	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/menucard_image/menucard_1764678284407_aea8da.jpg	t	2025-12-02 17:54:45.925452	2025-12-02 17:54:45.925452
9	3	\N	3/menucard_image/menucard_1764678285975_bc303e.jpg	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/menucard_image/menucard_1764678285975_bc303e.jpg	t	2025-12-02 17:54:47.125417	2025-12-02 17:54:47.125417
\.


--
-- Data for Name: merchant_products; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.merchant_products (product_id, merchant_id, product_name, category, description, price, stock_quantity, weight_kg, image_url, is_available, created_by, updated_by, created_on, updated_on, status, is_display) FROM stdin;
22	1	pizza	veg	good	299.00	200	200.00	https://aspireq-cloud-kitchen.s3.amazonaws.com/1/product_image/1761629049387_1581d64f.png	t	1	\N	2025-10-28 10:54:13.536821	2025-11-26 12:41:12.369134	active	t
23	1	pizza	veg	dfkjasdkfh	200.00	200	120.00	https://aspireq-cloud-kitchen.s3.amazonaws.com/1/product_image/1761629182898_5493c74d.png	t	1	\N	2025-10-28 10:56:26.559694	2025-11-26 12:41:12.369134	active	t
24	1	panner	veg	for ver is good	299.00	100	100.00	https://aspireq-cloud-kitchen.s3.amazonaws.com/1/product_image/1761630407230_dcde52e8.png	t	1	\N	2025-10-28 11:16:51.011637	2025-11-26 12:41:12.369134	active	t
28	1	pizza	veg	good	233.00	123	23.00	https://aspireq-cloud-kitchen.s3.amazonaws.com/1/product_image/1761637848207_70486265.png	t	1	\N	2025-10-28 13:20:51.89898	2025-11-26 12:41:12.369134	active	t
30	1	sadwich	main-course	good product	298.95	200	200.00	https://aspireq-cloud-kitchen.s3.amazonaws.com/1/product_image/1761638509588_016a015c.png	t	1	\N	2025-10-28 13:33:40.163821	2025-11-26 12:41:12.369134	active	t
7	1	pizza	veg	good for weat	154.00	171	156.00	https://aspireq-cloud-kitchen.s3.amazonaws.com/1/product_image/1762248041551_ae4af714.jpg	t	1	\N	2025-11-04 09:20:41.547977	2025-11-26 12:41:12.369134	active	t
9	1	panner	veg	 a popular Indian dish featuring marinated chunks of paneer (cottage cheese) and vegetables 	123.00	123	123.00	http://localhost:8082/uploads/1/product_image/1763718967984_0e58096f.png	t	1	\N	2025-11-21 15:26:07.822731	2025-11-26 12:41:12.369134	active	t
11	3	pizzaria	veg	pure veg prduct	200.00	100	\N	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/products/product_11.jpg	t	3	\N	2025-11-25 14:52:05.600509	2025-11-26 12:41:12.369134	active	t
10	3	pizza	veg	good for health	100.00	100	100.00	https://aspireq-cloud-kitchen.s3.amazonaws.com/products/product_10_1764581109825_6d67cd.png	t	3	3	2025-11-22 18:15:48.906153	2025-12-01 14:55:10.964968	active	t
12	3	dis Pizza	Food	Fresh and tasty pizza	299.99	50	0.50	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/products/product_12.jpg	t	3	\N	2025-12-01 16:01:47.308753	2025-12-01 16:01:47.327447	active	t
\.


--
-- Data for Name: merchant_website_assets; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.merchant_website_assets (id, merchant_id, asset_type, original_filename, s3_key, s3_url, file_size_bytes, mime_type, width_px, height_px, uploaded_at) FROM stdin;
1	3	LOGO	menu-card-img.jpg	3/logo/logo_1764584315121_50e0ff.jpg	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/logo/logo_1764584315121_50e0ff.jpg	235376	image/jpeg	\N	\N	2025-12-01 15:47:27.950699
\.


--
-- Data for Name: merchant_website_config; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.merchant_website_config (id, merchant_id, kitchen_name, subdomain, website_address, address, description, logo_url, banner_url, photo_url, whatsapp, instagram, youtube, twitter, facebook, is_published, website_s3_path, created_at, updated_at, published_at) FROM stdin;
1	3	firstkitchenforyou 	firstkitchenforyou	https://www.firstkitchenforyou.cloudkitchen.com/	firstkitchenforyou 	firstkitchenforyou 	\N	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/banner/Property 1=Default.png	https://aspireq-cloud-kitchen.s3.amazonaws.com/3/photo/PANNER IMAGE.png	8965471230					f	\N	2025-12-02 09:32:09.371074	2025-12-02 11:45:12.174367	\N
\.


--
-- Data for Name: merchant_website_theme; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.merchant_website_theme (id, merchant_id, primary_color, secondary_color, font_family, custom_css, template_name, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: merchants; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.merchants (merchant_id, merchant_name, business_name, business_type, website_url, phone, email, address, city, state, country, pincode, gstin, fssai_license, is_active, subscription_plan, subscription_expires_at, created_by, updated_by, created_on, updated_on) FROM stdin;
1	Yogeshs Kitchen	Yogeshs Kitchen	restaurant	\N	9075027009	yogeshs@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-10-27 17:37:43.412445	2025-10-27 17:37:43.412445
4	Fresh Kitchen	Fresh Kitchen	restaurant	\N	9123456789	fresh@kitchen.com	789 Fresh Street, Bangalore, Karnataka 560001	\N	\N	India	\N	\N	\N	t	basic	\N	0	\N	2025-11-07 18:02:28.634036	2025-11-07 18:02:28.634036
3	Yogesh Kitchenss	Yogesh Kitchens	restaurant	\N	8095242733	yogesh@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-11-05 16:26:01.610377	2025-11-12 18:10:47.651237
5	Spoon Kitchen	Spoon Kitchen	restaurant	\N	9876543210	spoon@kitchen.com	123 Main Street, City, State	\N	\N	India	\N	\N	\N	t	basic	\N	0	\N	2025-11-22 14:25:38.659353	2025-11-22 14:25:38.659353
6	diwana	diwana	restaurant	\N	9874563201	diwana@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-16 12:11:08.284018	2025-12-16 12:11:08.284018
7	Atuls Kitchen	Atuls Kitchen	restaurant	\N	8025242733	atul@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-25 22:29:44.534822	2025-12-25 22:29:44.534822
11	Atul's Kitchen	Atul's Kitchen	restaurant	\N	8525242733	atuls@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-25 22:31:03.286964	2025-12-25 22:31:03.286964
12	Akshay's Kitchen	Akshay's Kitchen	restaurant	\N	9025242733	Akshay@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-25 22:39:32.436861	2025-12-25 22:39:32.436861
13	shivaji	shivaji	restaurant	\N	9921607903	shivaji@gmail.com	raigaon tal	\N	\N	India	\N	22AAAAW2222A1Z3	12345678909876	t	basic	\N	0	\N	2025-12-25 23:23:53.146528	2025-12-25 23:23:53.146528
14	YogeshNagre	YogeshNagre	restaurant	\N	8095242743	yogesha@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-25 23:34:45.682814	2025-12-25 23:34:45.682814
15	SpiceHub	SpiceHub	restaurant	\N	9000000001	spicehub@gmail.com	Andheri East, Mumbai, Maharashtra 400069	\N	\N	India	\N	27SPICE1234F1Z1	10000000000001	t	basic	\N	0	\N	2025-12-25 23:36:49.67951	2025-12-25 23:36:49.67951
16	wertyu	wertyu	restaurant	\N	8410474741	hgj@gmail.com	sdftyuiuytfdfghj	\N	\N	India	\N	29ROYAL1234F1Z3	\N	t	basic	\N	0	\N	2025-12-25 23:59:46.485097	2025-12-25 23:59:46.485097
17	wertryt	wertryt	restaurant	\N	9546213021	wertyg@gmail.com	fghjhrewerghjhrew	\N	\N	India	\N	29ROYAL1234F1Z3	42541025412035	t	basic	\N	0	\N	2025-12-26 00:03:55.31894	2025-12-26 00:03:55.31894
18	dsfewr	dsfewr	restaurant	\N	8782525222	wer@gmai.com	dfsljafl;ak	\N	\N	India	\N	29ROYAL1234F1Z3	41258412587415	t	basic	\N	0	\N	2025-12-26 00:09:34.130245	2025-12-26 00:09:34.130245
19	SpiceHuba	SpiceHuba	restaurant	\N	9000000011	spicehuba@gmail.com	Andheri East, Mumbai, Maharashtra 400069	\N	\N	India	\N	27SPICE1234F1Z1	10000000000001	t	basic	\N	0	\N	2025-12-26 00:14:12.528603	2025-12-26 00:14:12.528603
20	UrbanTadka	UrbanTadka	restaurant	\N	9000000002	urbantadka@gmail.com	Bandra West, Mumbai, Maharashtra 400050	\N	\N	India	\N	27URBAN1234F1Z2	10000000000002	t	basic	\N	0	\N	2025-12-26 00:15:04.679948	2025-12-26 00:15:04.679948
21	Nagre Kitchen	Nagre Kitchen	restaurant	\N	8095242789	nagreh@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-26 11:34:16.583777	2025-12-26 11:34:16.583777
22	Nagres Kitchen	Nagres Kitchen	restaurant	\N	8095242782	nagrehs@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-26 11:40:08.607955	2025-12-26 11:40:08.607955
23	Nagreas Kitchen	Nagreas Kitchen	restaurant	\N	8095242722	nagrehas@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-26 11:44:31.997513	2025-12-26 11:44:31.997513
24	Nas Kitchen	Nas Kitchen	restaurant	\N	8095242727	ns@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-26 11:59:46.980874	2025-12-26 11:59:46.980874
25	Kitchen	Kitchen	restaurant	\N	8095247727	nys@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-26 12:03:49.152116	2025-12-26 12:03:49.152116
26	Yogi Kitchen	Yogi Kitchen	restaurant	\N	8098242733	yogi@gmail.com	123 Main Street, Mumbai, Maharashtra 400001	\N	\N	India	\N	29ABCDE1234F1Z5	12345678901234	t	basic	\N	0	\N	2025-12-26 12:49:03.347111	2025-12-26 12:49:03.347111
\.


--
-- Data for Name: offers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.offers (offer_id, merchant_id, offer_name, offer_number, offer_code, buy_quantity, get_quantity, offer_image_s3_key, offer_image_url, start_date, end_date, description, terms_and_conditions, discount_type, discount_value, max_discount_amount, min_purchase_amount, max_uses_per_customer, total_usage_limit, current_usage_count, status, is_active, is_featured, is_display, created_by_merchant_id, updated_by_merchant_id, created_on, updated_on, deleted_on) FROM stdin;
\.


--
-- Data for Name: order_filters; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.order_filters (filter_id, merchant_id, filter_name, from_date, to_date, selected_day, from_time, to_time, status_filters, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: order_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.order_items (item_id, order_id, product_id, product_name, product_sku, category_name, unit_price, quantity, subtotal, tax_amount, discount_amount, total_amount, special_instructions, created_at, updated_at) FROM stdin;
1	1	101	Paneer Tikka	SKU-PT-001	Starter	249.00	2	0.00	0.00	0.00	0.00	Less spicy	2025-12-18 18:02:37.70877	2025-12-18 18:02:37.70877
\.


--
-- Data for Name: order_items_staging; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.order_items_staging (item_id, order_id, product_id, product_name, product_sku, category_name, unit_price, quantity, subtotal, tax_amount, discount_amount, total_amount, special_instructions, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: order_status_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.order_status_history (history_id, order_id, from_status, to_status, changed_by, changed_by_role, notes, changed_at) FROM stdin;
1	1	NEW	PREPARING	\N	system	\N	2025-12-18 18:18:22.951763
2	1	PREPARING	OUT_FOR_DELIVERY	\N	system	\N	2025-12-18 18:20:25.721584
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.orders (order_id, merchant_id, customer_id, customer_name, customer_mobile, customer_email, delivery_address, delivery_city, delivery_pincode, delivery_latitude, delivery_longitude, order_status, placed_at, confirmed_at, preparing_at, dispatched_at, delivered_at, cancelled_at, subtotal, tax_amount, delivery_fee, discount_amount, total_amount, payment_method, payment_status, version, created_at, updated_at) FROM stdin;
1	1	1	John Doe	+91-9876543210	john@example.com	123 Main Street, Block A	Bangalore	560001	12.97160000	77.59460000	OUT_FOR_DELIVERY	2025-12-18 12:32:37.690058	2025-12-18 18:18:22.951763	2025-12-18 18:18:22.951763	2025-12-18 18:20:25.721584	\N	\N	0.00	0.00	0.00	0.00	0.00	COD	PENDING	2	2025-12-18 18:02:37.70877	2025-12-18 18:20:25.721584
\.


--
-- Data for Name: otp_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.otp_logs (otp_log_id, merchant_id, phone, otp_code, otp_type, status, ip_address, user_agent, attempts_count, verified_at, expires_at, created_on) FROM stdin;
6	\N	9999999999	6588	password_reset	sent	127.0.0.1	Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36	0	\N	2025-10-01 07:45:10.234276	2025-10-01 07:40:10.235275
7	\N	9999999999	6541	password_reset	sent	127.0.0.1	Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36	0	\N	2025-10-01 07:49:13.735532	2025-10-01 07:44:13.735532
21	\N	9876543210	6518	login	sent	127.0.0.1	curl/8.14.1	0	\N	2025-10-03 10:50:21.619998	2025-10-03 10:45:21.627535
22	\N	9876543210	****	login	verified	127.0.0.1	curl/8.14.1	0	2025-10-03 10:45:57.605186	2025-10-03 10:46:57.605186	2025-10-03 10:45:57.601149
23	\N	9999999999	4247	login	sent	127.0.0.1	curl/8.14.1	0	\N	2025-10-03 10:52:42.542025	2025-10-03 10:47:42.543024
24	\N	9999999999	****	login	verified	127.0.0.1	curl/8.14.1	0	2025-10-03 10:48:03.252487	2025-10-03 10:49:03.252487	2025-10-03 10:48:03.250489
31	1	9075027009	8280	password_reset	sent	127.0.0.1	Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	0	\N	2025-11-05 00:23:06.269415	2025-11-05 00:18:06.272951
32	1	9075027004	4314	login	sent	127.0.0.1	Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	0	\N	2025-12-23 11:50:01.643878	2025-12-23 11:45:01.669047
33	1	9075027004	9739	password_reset	sent	127.0.0.1	Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	0	\N	2025-12-23 12:53:24.215339	2025-12-23 12:48:24.215339
\.


--
-- Data for Name: permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.permissions (permission_id, permission_name, resource, action, description, created_by, updated_by, created_on, updated_on) FROM stdin;
1	users.create	users	create	Create new users	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
2	users.read	users	read	View user details	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
3	users.update	users	update	Update user information	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
4	users.delete	users	delete	Delete users	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
5	merchants.create	merchants	create	Create new merchants	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
6	merchants.read	merchants	read	View merchant details	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
7	merchants.update	merchants	update	Update merchant information	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
8	merchants.delete	merchants	delete	Delete merchants	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
9	roles.create	roles	create	Create new roles	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
10	roles.read	roles	read	View roles	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
11	roles.update	roles	update	Update roles	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
12	roles.delete	roles	delete	Delete roles	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
13	roles.assign	roles	assign	Assign roles to users	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
14	orders.create	orders	create	Create new orders	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
15	orders.read	orders	read	View orders	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
16	orders.update	orders	update	Update order status	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
17	orders.delete	orders	delete	Cancel/delete orders	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
18	products.create	products	create	Create new products	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
19	products.read	products	read	View products	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
20	products.update	products	update	Update product information	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
21	products.delete	products	delete	Delete products	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
22	payments.read	payments	read	View payment information	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
23	payments.process	payments	process	Process payments	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
24	payments.refund	payments	refund	Process refunds	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
25	user:read	user	read	Read user information	\N	\N	2025-10-06 17:21:57.253754	2025-10-06 17:21:57.253754
26	user:write	user	write	Create and update users	\N	\N	2025-10-06 17:21:57.253754	2025-10-06 17:21:57.253754
27	merchant:read	merchant	read	Read merchant information	\N	\N	2025-10-06 17:21:57.253754	2025-10-06 17:21:57.253754
28	merchant:write	merchant	write	Create and update merchants	\N	\N	2025-10-06 17:21:57.253754	2025-10-06 17:21:57.253754
29	offers.create	offers	create	Create offers	1	\N	2025-10-24 21:12:46.154243	2025-10-24 21:12:46.154243
30	offers.read	offers	read	View offers	1	\N	2025-10-24 21:12:46.154243	2025-10-24 21:12:46.154243
31	offers.update	offers	update	Update offers	1	\N	2025-10-24 21:12:46.154243	2025-10-24 21:12:46.154243
32	offers.delete	offers	delete	Delete offers	1	\N	2025-10-24 21:12:46.154243	2025-10-24 21:12:46.154243
\.


--
-- Data for Name: role_permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.role_permissions (role_permission_id, role_id, permission_id, created_by, updated_by, created_on, updated_on) FROM stdin;
1	1	1	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
2	1	2	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
3	1	3	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
4	1	4	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
5	1	5	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
6	1	6	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
7	1	7	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
8	1	8	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
9	1	9	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
10	1	10	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
11	1	11	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
12	1	12	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
13	1	13	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
14	1	14	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
15	1	15	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
16	1	16	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
17	1	17	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
18	1	18	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
19	1	19	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
20	1	20	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
21	1	21	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
22	1	22	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
23	1	23	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
24	1	24	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
25	2	1	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
26	2	2	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
27	2	3	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
28	2	4	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
29	2	6	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
30	2	7	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
31	2	9	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
32	2	10	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
33	2	11	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
34	2	12	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
35	2	13	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
36	2	14	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
37	2	15	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
38	2	16	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
39	2	17	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
40	2	18	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
41	2	19	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
42	2	20	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
43	2	21	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
44	2	22	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
45	2	23	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
46	2	24	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
47	5	14	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
48	5	15	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
49	5	19	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
50	5	3	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
51	1	28	\N	\N	2025-10-06 17:21:57.253754	2025-10-06 17:21:57.253754
52	1	26	\N	\N	2025-10-06 17:21:57.253754	2025-10-06 17:21:57.253754
53	1	25	\N	\N	2025-10-06 17:21:57.253754	2025-10-06 17:21:57.253754
54	1	27	\N	\N	2025-10-06 17:21:57.253754	2025-10-06 17:21:57.253754
55	2	25	\N	\N	2025-10-06 17:42:43.17253	2025-10-06 17:42:43.17253
56	2	26	\N	\N	2025-10-06 17:42:43.245144	2025-10-06 17:42:43.245144
57	1	29	1	\N	2025-10-24 21:13:10.396736	2025-10-24 21:13:10.396736
58	1	32	1	\N	2025-10-24 21:13:10.396736	2025-10-24 21:13:10.396736
59	1	30	1	\N	2025-10-24 21:13:10.396736	2025-10-24 21:13:10.396736
60	1	31	1	\N	2025-10-24 21:13:10.396736	2025-10-24 21:13:10.396736
61	2	29	1	\N	2025-10-24 21:13:10.396736	2025-10-24 21:13:10.396736
62	2	32	1	\N	2025-10-24 21:13:10.396736	2025-10-24 21:13:10.396736
63	2	30	1	\N	2025-10-24 21:13:10.396736	2025-10-24 21:13:10.396736
64	2	31	1	\N	2025-10-24 21:13:10.396736	2025-10-24 21:13:10.396736
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.roles (role_id, role_name, description, is_system_role, created_by, updated_by, created_on, updated_on) FROM stdin;
1	super_admin	Super Administrator with full system access	t	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
3	merchant_manager	Merchant Manager with limited admin access	t	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
4	merchant_staff	Merchant Staff with operational access	t	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
5	customer	Customer with order and profile access	t	1	\N	2025-09-30 12:14:33.90136	2025-09-30 12:14:33.90136
2	merchant	Merchant Administrator with full merchant access	t	1	\N	2025-09-30 12:14:33.90136	2025-10-08 10:35:40.512259
\.


--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_roles (user_role_id, user_id, role_id, merchant_id, assigned_at, expires_at, created_by, updated_by, created_on, updated_on) FROM stdin;
24	1	1	\N	2025-10-16 16:35:25.957863	\N	1	1	2025-10-16 16:35:25.957863	2025-10-16 16:35:25.957863
25	7	5	1	2025-11-24 18:42:12.082753	\N	\N	\N	2025-11-24 18:42:10.784259	2025-11-24 18:42:10.784259
26	8	5	3	2025-12-12 15:20:46.044083	\N	\N	\N	2025-12-12 15:20:44.95649	2025-12-12 15:20:44.95649
27	10	5	1	2025-12-18 16:28:52.756902	\N	\N	\N	2025-12-18 16:28:51.544004	2025-12-18 16:28:51.544004
29	14	5	1	2025-12-22 14:13:18.97928	\N	\N	\N	2025-12-22 14:13:03.441585	2025-12-22 14:13:03.441585
30	15	5	1	2025-12-22 16:04:39.061043	\N	\N	\N	2025-12-22 16:04:38.075213	2025-12-22 16:04:38.075213
31	33	5	3	2025-12-27 12:38:05.932746	\N	\N	\N	2025-12-27 12:38:05.192509	2025-12-27 12:38:05.192509
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (user_id, merchant_id, phone, email, username, password_hash, first_name, last_name, date_of_birth, gender, profile_image_url, user_type, is_active, is_verified, phone_verified, email_verified, last_login_at, password_reset_token, password_reset_expires_at, email_verification_token, email_verified_at, otp_code, otp_expires_at, otp_attempts, otp_blocked_until, is_guest, guest_converted_at, preferred_login_method, created_by, updated_by, created_on, updated_on, address, dietary_preferences, favorite_food, otp_used) FROM stdin;
5	4	9123456789	fresh@kitchen.com	freshkitchen	$2a$12$Y87mzWmV8gw4sc2280YK2ezE8xcoHEJoDhIsyZNH9wAvjRAGoeZcy	Fresh Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-11-07 18:02:28.605977	2025-11-07 18:02:28.605977	\N	\N	\N	f
31	25	8095247727	nys@gmail.com	nay_kitchen	$2a$12$BYneCm6h5oEfPpqigJYg.uT2OdVWipce.NyNJF0gcTiH.PCAXmj6i	Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 12:03:49.146807	2025-12-26 12:03:49.146807	\N	\N	\N	f
3	1	9075027009	yogeshs@gmail.com	yogesh_kitchen	$2a$12$3OtyDGuoGg0ExIDQrKbkRO9Hc9XZxDAk6DOnWSJfu7rCE9hUyTvWi	Yogeshs Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-10-27 17:37:43.393559	2025-11-13 16:17:14.906901	\N	\N	\N	f
6	5	9876543210	spoon@kitchen.com	spoonkitchen	$2a$12$D88rb4Pgf.BCh06w.Rq5BOscM5Hv6qdiotmzkkknNhs3dakBM89P6	Spoon Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-11-22 14:25:38.654662	2025-11-22 14:25:38.654662	\N	\N	\N	f
7	1	9874562130	yogesh@example.com	9874562130	$2a$12$mSlCdda7qShds2T0eqrvxedoxAPRFT6mubvIj4UvPJ9bsGWAq1MwS	Yogesh	Nagre	\N	\N	\N	customer	t	f	f	f	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	\N	\N	2025-11-24 18:42:10.784259	2025-11-24 18:42:10.784259	123 Main Street	\N	\N	f
8	3	9921607903	johnydeep@example.com	9921607903	$2a$12$8z2IuUlGBaLR188ZkOuSZ.WU.nYPeeCRPQ5dt1YS4Z7Z2SNLxn6Je	yogesh	Nagre	\N	\N	\N	customer	t	f	f	f	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	\N	\N	2025-12-12 15:20:44.95649	2025-12-12 15:20:44.95649	123 Main Street	\N	\N	f
9	6	9874563201	diwana@gmail.com	diwana	$2a$12$mEgUpemxwrTWVvHcmRq3/.y2tTLwdRKUFFycCmu8yTH1IBpuyKb.m	diwana	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-16 12:11:08.253524	2025-12-16 12:11:08.253524	\N	\N	\N	f
1	\N	9999999999	admin@cloudkitchen.com	admin	$2a$12$TSOfcTruUeZWxM/QEmWbHuS.Z5bEQkNL93o0bVAvzrvgmbZ8yy//O	Super	Admin	\N	male	\N	super_admin	t	f	f	f	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	1	1	2025-10-16 16:35:15.284132	2025-10-16 16:35:15.284132	\N	\N	\N	\N
32	26	8098242733	yogi@gmail.com	yogeshwarn	$2a$12$V6uUVmvZHHB5xArxFzrHxO3FRr2um.w09V2N8qSiwIONaOsLulFZq	Yogi Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 12:49:03.337817	2025-12-26 12:49:03.337817	\N	\N	\N	f
4	3	8095242734	yogeshsn@gmail.com	yogesh	$2a$12$7lPq4YSgTBdNVqPTjUTieOAAhFgRa8vGhYpKeCQ8ibdrzpqcdZKSi	Yogesh Kitchens	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-11-05 16:26:01.605691	2025-11-05 16:26:01.605691	\N	\N	\N	f
14	1	9075027104	yogesh@email.com	9075027104	$2a$12$F21JiTYbLva3aD0Yk.HEm.g81yPs6neUExzuGuA4Sud6f8MruKCdy	Yogesh	Doe	\N	\N	\N	customer	t	f	f	f	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	\N	\N	2025-12-22 14:13:03.441585	2025-12-22 14:13:03.441585	123 Main Street	\N	\N	f
15	1	9075027204	\N	9075027204	$2a$12$.INHZGJFA./PZ8CHk6WMFedrRtbJ/4SV9yV66DjnfwjsKCX/y1tWq	John	Doe	\N	\N	\N	customer	t	f	f	f	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	\N	\N	2025-12-22 16:04:38.075213	2025-12-22 16:04:38.075213	123 Main Street	\N	\N	f
10	1	9075027004	john@example.com	9075027004	$2a$12$7iEIKHai7lAsJiGxmf35yuRgmJQc0w84Us11xuwZt1LarpCZNdJEq	Yogesh	Nagre	\N	\N	\N	customer	t	f	f	f	\N	\N	\N	\N	\N	9739	2025-12-23 12:53:24.215339	0	\N	f	\N	otp	\N	\N	2025-12-18 16:28:51.544004	2025-12-23 12:48:24.189948	123 Main Street	\N	\N	f
16	7	8025242733	atul@gmail.com	atul_kitchen	$2a$12$ORknQPw49Tk3RwBckD16wuGh7bMYg7r3Vf/b5h/MVzdaDAkWbZuvi	Atuls Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-25 22:29:44.511664	2025-12-25 22:29:44.511664	\N	\N	\N	f
17	11	8525242733	atuls@gmail.com	atuls	$2a$12$Lf4Tm/LRJwp3590puRBqNOLS4DSDpZ95b9dfH9tZv52oEyTlbyGVO	Atul's Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-25 22:31:03.282915	2025-12-25 22:31:03.282915	\N	\N	\N	f
18	12	9025242733	Akshay@gmail.com	Akshay	$2a$12$KEHJclh3rL6ikIM9NBDsOuaKYCZO05RVldZ0Wwgy2rne4Lzbz3jqq	Akshay's Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-25 22:39:32.418023	2025-12-25 22:39:32.418023	\N	\N	\N	f
19	13	9921607903	shivaji@gmail.com	shivaji	$2a$12$1ZMQaFAIKTJFlx/IFQbQu.C8FX4QAgoshbhvGVfeMFsI7GTi0NX8G	shivaji	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-25 23:23:53.13639	2025-12-25 23:23:53.13639	\N	\N	\N	f
20	14	8095242743	yogesha@gmail.com	yogeshnagre	$2a$12$ffnzu7NyBd4egoplirIc1.xdEmYC5G88.oYORSIRGZU.xXdKx0QfO	YogeshNagre	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-25 23:34:45.67483	2025-12-25 23:34:45.67483	\N	\N	\N	f
21	15	9000000001	spicehub@gmail.com	spicehub	$2a$12$2Q.f0oFUhcaNuGcrZcWfW.2G4uO78GqOg1Cw3Ffh5BVS888xtHkzW	SpiceHub	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-25 23:36:49.666962	2025-12-25 23:36:49.666962	\N	\N	\N	f
22	16	8410474741	hgj@gmail.com	dfghjhgf	$2a$12$.iYNYUz8c5zSuRkyIZ7TV.MAVNm2O6.Pj2Fls/u3dskPpgZjCZOsC	wertyu	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-25 23:59:46.47501	2025-12-25 23:59:46.47501	\N	\N	\N	f
23	17	9546213021	wertyg@gmail.com	djfcnkljdf	$2a$12$4UNWRoBiT2EB5VEGYP/Wf.mtupklwO51gnxi3OOvIKnjs9tWfr.LW	wertryt	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 00:03:55.301969	2025-12-26 00:03:55.301969	\N	\N	\N	f
24	18	8782525222	wer@gmai.com	jkdlf	$2a$12$NrI/C51rZMk88Pve/UkDH.CMSefD4yQBDARj1iq9Kgrmp3NuNUqs6	dsfewr	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 00:09:34.124273	2025-12-26 00:09:34.124273	\N	\N	\N	f
25	19	9000000011	spicehuba@gmail.com	spicehuba	$2a$12$tl6qCw.mq01RrBhr0lkwPOZqazeus3FC47/h4XMi1z6TxaVk694/.	SpiceHuba	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 00:14:12.498378	2025-12-26 00:14:12.498378	\N	\N	\N	f
26	20	9000000002	urbantadka@gmail.com	urbantadka	$2a$12$9S3D1I2r4D0/p0bW0/DG3.BzsJdd3pasLnAsOHSdTkFbhrXcaej5K	UrbanTadka	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 00:15:04.635999	2025-12-26 00:15:04.635999	\N	\N	\N	f
27	21	8095242789	nagreh@gmail.com	nagre_kitchen	$2a$12$ZhPvp.DjCv2ZdlmyckTaFumOJ5ggvfc4HwwzYJCPIzbj9KUKQvafm	Nagre Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 11:34:16.562314	2025-12-26 11:34:16.562314	\N	\N	\N	f
28	22	8095242782	nagrehs@gmail.com	nagres_kitchen	$2a$12$SC1JFRCdSa0pxczkjxZSLuUDKpHoqbzAfwRHVZKK6jbkuHC/0cpae	Nagres Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 11:40:08.540934	2025-12-26 11:40:08.540934	\N	\N	\N	f
29	23	8095242722	nagrehas@gmail.com	nagreas_kitchen	$2a$12$t7WNkc3eYtuZ4l7yRBxIruW2ma2ALhFqyOv8FkDZKqxA1Z/ZSRfk2	Nagreas Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 11:44:31.988864	2025-12-26 11:44:31.988864	\N	\N	\N	f
30	24	8095242727	ns@gmail.com	na_kitchen	$2a$12$zY78L5lqBtCxfCuq0HGiyuSX9uEg0msyPhsxYDisXsrzLPnmoRVIe	Nas Kitchen	Admin	\N	\N	\N	merchant	t	f	t	t	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	0	\N	2025-12-26 11:59:46.976653	2025-12-26 11:59:46.976653	\N	\N	\N	f
33	3	9075027004	\N	9075027004	$2a$12$PXmOYDE39oXhNFHHxyaol.Et.2ksOdW9eJA6cjH.8hfpgQrNlgU0a	Yoges	Nagre	\N	\N	\N	customer	t	f	f	f	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	otp	\N	\N	2025-12-27 12:38:05.192509	2025-12-27 12:38:05.192509	123 Main Street	\N	\N	f
\.


--
-- Name: ads_ad_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ads_ad_id_seq', 15, true);


--
-- Name: customers_customer_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.customers_customer_id_seq', 4, true);


--
-- Name: image_records_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.image_records_id_seq', 1, false);


--
-- Name: menu_cards_menu_card_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.menu_cards_menu_card_id_seq', 9, true);


--
-- Name: merchant_products_product_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.merchant_products_product_id_seq', 12, true);


--
-- Name: merchant_website_assets_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.merchant_website_assets_id_seq', 1, true);


--
-- Name: merchant_website_config_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.merchant_website_config_id_seq', 1, true);


--
-- Name: merchant_website_theme_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.merchant_website_theme_id_seq', 1, false);


--
-- Name: merchants_merchant_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.merchants_merchant_id_seq', 26, true);


--
-- Name: offers_offer_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.offers_offer_id_seq', 1, false);


--
-- Name: order_filters_filter_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.order_filters_filter_id_seq', 1, false);


--
-- Name: order_items_item_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.order_items_item_id_seq', 1, true);


--
-- Name: order_status_history_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.order_status_history_history_id_seq', 2, true);


--
-- Name: orders_order_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.orders_order_id_seq', 1, true);


--
-- Name: otp_logs_otp_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.otp_logs_otp_log_id_seq', 33, true);


--
-- Name: permissions_permission_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.permissions_permission_id_seq', 32, true);


--
-- Name: role_permissions_role_permission_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.role_permissions_role_permission_id_seq', 64, true);


--
-- Name: roles_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.roles_role_id_seq', 6, true);


--
-- Name: user_roles_user_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_roles_user_role_id_seq', 31, true);


--
-- Name: users_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_user_id_seq', 33, true);


--
-- Name: ads ads_ad_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ads
    ADD CONSTRAINT ads_ad_number_key UNIQUE (ad_number);


--
-- Name: ads ads_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ads
    ADD CONSTRAINT ads_pkey PRIMARY KEY (ad_id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (customer_id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: image_records image_records_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.image_records
    ADD CONSTRAINT image_records_pkey PRIMARY KEY (id);


--
-- Name: menu_cards menu_cards_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.menu_cards
    ADD CONSTRAINT menu_cards_pkey PRIMARY KEY (menu_card_id);


--
-- Name: merchant_products merchant_products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_products
    ADD CONSTRAINT merchant_products_pkey PRIMARY KEY (product_id);


--
-- Name: merchant_website_assets merchant_website_assets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_website_assets
    ADD CONSTRAINT merchant_website_assets_pkey PRIMARY KEY (id);


--
-- Name: merchant_website_config merchant_website_config_merchant_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_website_config
    ADD CONSTRAINT merchant_website_config_merchant_id_key UNIQUE (merchant_id);


--
-- Name: merchant_website_config merchant_website_config_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_website_config
    ADD CONSTRAINT merchant_website_config_pkey PRIMARY KEY (id);


--
-- Name: merchant_website_theme merchant_website_theme_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_website_theme
    ADD CONSTRAINT merchant_website_theme_pkey PRIMARY KEY (id);


--
-- Name: merchants merchants_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_email_key UNIQUE (email);


--
-- Name: merchants merchants_merchant_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_merchant_name_key UNIQUE (merchant_name);


--
-- Name: merchants merchants_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_pkey PRIMARY KEY (merchant_id);


--
-- Name: offers offers_offer_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offers
    ADD CONSTRAINT offers_offer_code_key UNIQUE (offer_code);


--
-- Name: offers offers_offer_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offers
    ADD CONSTRAINT offers_offer_number_key UNIQUE (offer_number);


--
-- Name: offers offers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offers
    ADD CONSTRAINT offers_pkey PRIMARY KEY (offer_id);


--
-- Name: order_filters order_filters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_filters
    ADD CONSTRAINT order_filters_pkey PRIMARY KEY (filter_id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (item_id);


--
-- Name: order_items_staging order_items_staging_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items_staging
    ADD CONSTRAINT order_items_staging_pkey PRIMARY KEY (item_id);


--
-- Name: order_status_history order_status_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_status_history
    ADD CONSTRAINT order_status_history_pkey PRIMARY KEY (history_id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (order_id);


--
-- Name: otp_logs otp_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.otp_logs
    ADD CONSTRAINT otp_logs_pkey PRIMARY KEY (otp_log_id);


--
-- Name: permissions permissions_permission_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_permission_name_key UNIQUE (permission_name);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (permission_id);


--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_permission_id);


--
-- Name: role_permissions role_permissions_role_id_permission_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_role_id_permission_id_key UNIQUE (role_id, permission_id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);


--
-- Name: roles roles_role_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_role_name_key UNIQUE (role_name);


--
-- Name: users uk_users_merchant_phone; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_merchant_phone UNIQUE (merchant_id, phone);


--
-- Name: user_roles ukh0v87oy7edfx4clbh1wml3vjw; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT ukh0v87oy7edfx4clbh1wml3vjw UNIQUE (user_id, role_id, merchant_id);


--
-- Name: role_permissions ukt43p6aampim70fxxnkid1mibj; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT ukt43p6aampim70fxxnkid1mibj UNIQUE (role_id, permission_id);


--
-- Name: customers uq_customer_merchant_phone; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT uq_customer_merchant_phone UNIQUE (merchant_id, phone);


--
-- Name: order_filters uq_order_filters_merchant_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_filters
    ADD CONSTRAINT uq_order_filters_merchant_name UNIQUE (merchant_id, filter_name);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_role_id);


--
-- Name: user_roles user_roles_user_id_role_id_merchant_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_role_id_merchant_id_key UNIQUE (user_id, role_id, merchant_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_ads_ad_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ads_ad_code ON public.ads USING btree (ad_code);


--
-- Name: idx_ads_ad_number; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ads_ad_number ON public.ads USING btree (ad_number);


--
-- Name: idx_ads_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ads_code ON public.ads USING btree (ad_code);


--
-- Name: idx_ads_created_by; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ads_created_by ON public.ads USING btree (created_by);


--
-- Name: idx_ads_dates; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ads_dates ON public.ads USING btree (from_date, to_date);


--
-- Name: idx_ads_dates_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ads_dates_status ON public.ads USING btree (from_date, to_date, display_status);


--
-- Name: idx_ads_display_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ads_display_status ON public.ads USING btree (display_status);


--
-- Name: idx_customers_merchant_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_merchant_active ON public.customers USING btree (merchant_id, is_active) WHERE (deleted_at IS NULL);


--
-- Name: idx_customers_merchant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_merchant_id ON public.customers USING btree (merchant_id);


--
-- Name: idx_customers_phone; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_phone ON public.customers USING btree (phone);


--
-- Name: idx_customers_profile_image; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_profile_image ON public.customers USING btree (profile_image_url);


--
-- Name: idx_customers_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_user_id ON public.customers USING btree (user_id);


--
-- Name: idx_customers_user_merchant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_user_merchant ON public.customers USING btree (user_id, merchant_id);


--
-- Name: INDEX idx_customers_user_merchant; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON INDEX public.idx_customers_user_merchant IS 'Optimizes customer profile queries - improves customer data retrieval';


--
-- Name: idx_menu_cards_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_menu_cards_active ON public.menu_cards USING btree (merchant_id, is_active);


--
-- Name: idx_menu_cards_merchant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_menu_cards_merchant ON public.menu_cards USING btree (merchant_id);


--
-- Name: idx_merchant_asset; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_asset ON public.merchant_website_assets USING btree (merchant_id, asset_type);


--
-- Name: idx_merchant_products_category; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_products_category ON public.merchant_products USING btree (category);


--
-- Name: idx_merchant_products_display; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_products_display ON public.merchant_products USING btree (is_display);


--
-- Name: idx_merchant_products_is_available; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_products_is_available ON public.merchant_products USING btree (is_available);


--
-- Name: idx_merchant_products_merchant_available; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_products_merchant_available ON public.merchant_products USING btree (merchant_id, is_available);


--
-- Name: idx_merchant_products_merchant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_products_merchant_id ON public.merchant_products USING btree (merchant_id);


--
-- Name: idx_merchant_products_merchant_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_products_merchant_status ON public.merchant_products USING btree (merchant_id, status);


--
-- Name: idx_merchant_products_merchant_status_available; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_products_merchant_status_available ON public.merchant_products USING btree (merchant_id, status, is_available);


--
-- Name: idx_merchant_products_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_products_status ON public.merchant_products USING btree (status);


--
-- Name: idx_merchant_website_assets_merchant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_website_assets_merchant_id ON public.merchant_website_assets USING btree (merchant_id);


--
-- Name: idx_merchant_website_assets_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_website_assets_type ON public.merchant_website_assets USING btree (asset_type);


--
-- Name: idx_merchant_website_config_published; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_website_config_published ON public.merchant_website_config USING btree (is_published);


--
-- Name: idx_merchant_website_config_website_address; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchant_website_config_website_address ON public.merchant_website_config USING btree (website_address);


--
-- Name: idx_merchants_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchants_active ON public.merchants USING btree (is_active);


--
-- Name: idx_merchants_is_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchants_is_active ON public.merchants USING btree (is_active);


--
-- Name: INDEX idx_merchants_is_active; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON INDEX public.idx_merchants_is_active IS 'Optimizes merchant status filtering - improves list performance';


--
-- Name: idx_merchants_phone; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_merchants_phone ON public.merchants USING btree (phone);


--
-- Name: INDEX idx_merchants_phone; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON INDEX public.idx_merchants_phone IS 'Optimizes merchant uniqueness checks - speeds up merchant creation';


--
-- Name: idx_offers_active_range; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_offers_active_range ON public.offers USING btree (merchant_id, status, start_date, end_date);


--
-- Name: idx_offers_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_offers_code ON public.offers USING btree (offer_code);


--
-- Name: idx_offers_dates; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_offers_dates ON public.offers USING btree (start_date, end_date);


--
-- Name: idx_offers_merchant_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_offers_merchant_status ON public.offers USING btree (merchant_id, status);


--
-- Name: idx_order_items_order_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_order_items_order_id ON public.order_items USING btree (order_id);


--
-- Name: idx_order_status_history_order_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_order_status_history_order_id ON public.order_status_history USING btree (order_id);


--
-- Name: idx_orders_customer_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_orders_customer_active ON public.orders USING btree (customer_id, placed_at DESC) WHERE ((order_status)::text <> 'CANCELLED'::text);


--
-- Name: idx_orders_customer_placed_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_orders_customer_placed_status ON public.orders USING btree (customer_id, placed_at DESC, order_status);


--
-- Name: idx_orders_merchant_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_orders_merchant_active ON public.orders USING btree (merchant_id, placed_at DESC) WHERE ((order_status)::text = ANY ((ARRAY['NEW'::character varying, 'PREPARING'::character varying, 'OUT_FOR_DELIVERY'::character varying])::text[]));


--
-- Name: idx_orders_merchant_placed_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_orders_merchant_placed_status ON public.orders USING btree (merchant_id, placed_at DESC, order_status);


--
-- Name: idx_orders_payment_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_orders_payment_status ON public.orders USING btree (payment_status);


--
-- Name: idx_otp_logs_created_on; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_logs_created_on ON public.otp_logs USING btree (created_on);


--
-- Name: idx_otp_logs_expires_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_logs_expires_at ON public.otp_logs USING btree (expires_at);


--
-- Name: idx_otp_logs_merchant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_logs_merchant_id ON public.otp_logs USING btree (merchant_id);


--
-- Name: idx_otp_logs_merchant_phone; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_logs_merchant_phone ON public.otp_logs USING btree (merchant_id, phone);


--
-- Name: idx_otp_logs_phone; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_logs_phone ON public.otp_logs USING btree (phone);


--
-- Name: idx_otp_logs_phone_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_logs_phone_created ON public.otp_logs USING btree (phone, created_on);


--
-- Name: INDEX idx_otp_logs_phone_created; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON INDEX public.idx_otp_logs_phone_created IS 'Optimizes OTP rate limiting queries - prevents sequential scans';


--
-- Name: idx_otp_logs_phone_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_logs_phone_status ON public.otp_logs USING btree (phone, status);


--
-- Name: idx_permissions_resource_action; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_permissions_resource_action ON public.permissions USING btree (resource, action);


--
-- Name: idx_products_category; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_category ON public.merchant_products USING btree (category, is_available);


--
-- Name: idx_products_merchant_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_merchant_status ON public.merchant_products USING btree (merchant_id, status, is_available);


--
-- Name: idx_role_permissions_permission_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_role_permissions_permission_id ON public.role_permissions USING btree (permission_id);


--
-- Name: idx_user_roles_merchant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_roles_merchant_id ON public.user_roles USING btree (merchant_id);


--
-- Name: idx_user_roles_merchant_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_roles_merchant_user ON public.user_roles USING btree (merchant_id, user_id);


--
-- Name: idx_user_roles_role_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_roles_role_id ON public.user_roles USING btree (role_id);


--
-- Name: idx_user_roles_user_merchant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_roles_user_merchant ON public.user_roles USING btree (user_id, merchant_id);


--
-- Name: INDEX idx_user_roles_user_merchant; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON INDEX public.idx_user_roles_user_merchant IS 'Optimizes token generation queries - reduces role/permission lookup time';


--
-- Name: idx_users_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_active ON public.users USING btree (is_active);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_global_email_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_global_email_unique ON public.users USING btree (email) WHERE ((merchant_id IS NULL) AND (email IS NOT NULL));


--
-- Name: idx_users_global_phone_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_global_phone_unique ON public.users USING btree (phone) WHERE (merchant_id IS NULL);


--
-- Name: idx_users_global_username_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_global_username_unique ON public.users USING btree (username) WHERE ((merchant_id IS NULL) AND (username IS NOT NULL));


--
-- Name: idx_users_is_guest; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_is_guest ON public.users USING btree (is_guest);


--
-- Name: idx_users_merchant_email_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_merchant_email_unique ON public.users USING btree (merchant_id, email) WHERE (email IS NOT NULL);


--
-- Name: idx_users_merchant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_merchant_id ON public.users USING btree (merchant_id);


--
-- Name: idx_users_merchant_phone_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_merchant_phone_unique ON public.users USING btree (merchant_id, phone);


--
-- Name: idx_users_merchant_username_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_merchant_username_unique ON public.users USING btree (merchant_id, username) WHERE (username IS NOT NULL);


--
-- Name: idx_users_merchant_usertype; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_merchant_usertype ON public.users USING btree (merchant_id, user_type);


--
-- Name: INDEX idx_users_merchant_usertype; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON INDEX public.idx_users_merchant_usertype IS 'Optimizes merchant listing queries - eliminates N+1 problem';


--
-- Name: idx_users_otp_expires; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_otp_expires ON public.users USING btree (otp_expires_at);


--
-- Name: idx_users_phone; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_phone ON public.users USING btree (phone);


--
-- Name: idx_users_phone_merchant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_phone_merchant ON public.users USING btree (phone, merchant_id);


--
-- Name: INDEX idx_users_phone_merchant; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON INDEX public.idx_users_phone_merchant IS 'Optimizes login and OTP verification queries - reduces query time by 70-90%';


--
-- Name: idx_users_phone_verified; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_phone_verified ON public.users USING btree (phone_verified);


--
-- Name: idx_users_user_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_user_type ON public.users USING btree (user_type);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: uq_customer_merchant_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_customer_merchant_user ON public.customers USING btree (merchant_id, user_id) WHERE (user_id IS NOT NULL);


--
-- Name: orders trg_log_order_status_change; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_log_order_status_change BEFORE UPDATE ON public.orders FOR EACH ROW EXECUTE FUNCTION public.log_order_status_change();


--
-- Name: order_filters trg_update_order_filters_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_order_filters_updated_at BEFORE UPDATE ON public.order_filters FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: order_items trg_update_order_items_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_order_items_updated_at BEFORE UPDATE ON public.order_items FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: order_items trg_update_order_totals; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_order_totals AFTER INSERT OR DELETE OR UPDATE ON public.order_items FOR EACH ROW EXECUTE FUNCTION public.update_order_totals();


--
-- Name: orders trg_update_orders_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_orders_updated_at BEFORE UPDATE ON public.orders FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: ads update_ads_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_ads_updated_at BEFORE UPDATE ON public.ads FOR EACH ROW EXECUTE FUNCTION public.update_ads_updated_at();


--
-- Name: customers update_customers_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_customers_updated_on BEFORE UPDATE ON public.customers FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: menu_cards update_menu_cards_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_menu_cards_updated_on BEFORE UPDATE ON public.menu_cards FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: merchant_products update_merchant_products_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_merchant_products_updated_on BEFORE UPDATE ON public.merchant_products FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: merchant_website_config update_merchant_website_config_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_merchant_website_config_updated_at BEFORE UPDATE ON public.merchant_website_config FOR EACH ROW EXECUTE FUNCTION public.update_website_config_updated_at();


--
-- Name: merchants update_merchants_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_merchants_updated_on BEFORE UPDATE ON public.merchants FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: offers update_offers_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_offers_updated_on BEFORE UPDATE ON public.offers FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: permissions update_permissions_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_permissions_updated_on BEFORE UPDATE ON public.permissions FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: role_permissions update_role_permissions_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_role_permissions_updated_on BEFORE UPDATE ON public.role_permissions FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: roles update_roles_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_roles_updated_on BEFORE UPDATE ON public.roles FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: user_roles update_user_roles_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_user_roles_updated_on BEFORE UPDATE ON public.user_roles FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: users update_users_updated_on; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_users_updated_on BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_on_column();


--
-- Name: customers customers_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(merchant_id) ON DELETE CASCADE;


--
-- Name: customers customers_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE SET NULL;


--
-- Name: merchant_website_config fk_merchant; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_website_config
    ADD CONSTRAINT fk_merchant FOREIGN KEY (merchant_id) REFERENCES public.merchants(merchant_id) ON DELETE CASCADE;


--
-- Name: merchant_products merchant_products_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.merchant_products
    ADD CONSTRAINT merchant_products_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(merchant_id) ON DELETE CASCADE;


--
-- Name: offers offers_created_by_merchant_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offers
    ADD CONSTRAINT offers_created_by_merchant_fkey FOREIGN KEY (created_by_merchant_id) REFERENCES public.merchants(merchant_id) ON DELETE SET NULL;


--
-- Name: offers offers_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offers
    ADD CONSTRAINT offers_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(merchant_id) ON DELETE CASCADE;


--
-- Name: offers offers_updated_by_merchant_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offers
    ADD CONSTRAINT offers_updated_by_merchant_fkey FOREIGN KEY (updated_by_merchant_id) REFERENCES public.merchants(merchant_id) ON DELETE SET NULL;


--
-- Name: order_filters order_filters_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_filters
    ADD CONSTRAINT order_filters_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(merchant_id);


--
-- Name: order_items order_items_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(order_id) ON DELETE CASCADE;


--
-- Name: order_status_history order_status_history_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_status_history
    ADD CONSTRAINT order_status_history_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(order_id) ON DELETE CASCADE;


--
-- Name: orders orders_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(customer_id);


--
-- Name: orders orders_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(merchant_id);


--
-- Name: otp_logs otp_logs_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.otp_logs
    ADD CONSTRAINT otp_logs_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(merchant_id) ON DELETE CASCADE;


--
-- Name: role_permissions role_permissions_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions(permission_id) ON DELETE CASCADE;


--
-- Name: role_permissions role_permissions_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(role_id) ON DELETE CASCADE;


--
-- Name: user_roles user_roles_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(merchant_id) ON DELETE CASCADE;


--
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(role_id) ON DELETE CASCADE;


--
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: users users_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(merchant_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict KEapfE5DoifF1E60xRBXyHwHi0nSIJ7jaxYJHTlWphk3xalFDnSZRLezw843qU3

