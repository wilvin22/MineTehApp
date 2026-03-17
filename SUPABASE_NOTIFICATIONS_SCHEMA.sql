-- Notifications System Database Schema
-- Run this in Supabase SQL Editor

-- Create notifications table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES accounts(account_id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create notification_preferences table
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES accounts(account_id) ON DELETE CASCADE UNIQUE,
    bid_placed_enabled BOOLEAN DEFAULT TRUE,
    bid_outbid_enabled BOOLEAN DEFAULT TRUE,
    auction_ending_enabled BOOLEAN DEFAULT TRUE,
    auction_won_enabled BOOLEAN DEFAULT TRUE,
    auction_lost_enabled BOOLEAN DEFAULT TRUE,
    item_sold_enabled BOOLEAN DEFAULT TRUE,
    new_message_enabled BOOLEAN DEFAULT TRUE,
    listing_approved_enabled BOOLEAN DEFAULT TRUE,
    payment_received_enabled BOOLEAN DEFAULT TRUE,
    push_notifications_enabled BOOLEAN DEFAULT TRUE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for performance optimization
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = false;
CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);

-- Enable Row Level Security
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;

-- RLS Policies for notifications
CREATE POLICY "Users can view their own notifications"
ON notifications FOR SELECT
USING (true);

CREATE POLICY "System can insert notifications"
ON notifications FOR INSERT
WITH CHECK (true);

CREATE POLICY "Users can update their own notifications"
ON notifications FOR UPDATE
USING (true);

-- RLS Policies for notification preferences
CREATE POLICY "Users can view their own preferences"
ON notification_preferences FOR SELECT
USING (true);

CREATE POLICY "Users can insert their own preferences"
ON notification_preferences FOR INSERT
WITH CHECK (true);

CREATE POLICY "Users can update their own preferences"
ON notification_preferences FOR UPDATE
USING (true);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_notifications_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers to auto-update updated_at
CREATE TRIGGER update_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_notifications_updated_at_column();

CREATE TRIGGER update_notification_preferences_updated_at
    BEFORE UPDATE ON notification_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_notifications_updated_at_column();

-- Function to create bid notifications
CREATE OR REPLACE FUNCTION create_bid_notification()
RETURNS TRIGGER AS $$
BEGIN
    -- Notify seller of new bid
    INSERT INTO notifications (user_id, type, title, message, data)
    SELECT 
        l.seller_id,
        'BID_PLACED',
        'New bid on your item',
        'Someone placed a bid of ₱' || NEW.bid_amount || ' on your ' || l.title,
        json_build_object(
            'listing_id', NEW.listing_id,
            'bid_id', NEW.bid_id,
            'bid_amount', NEW.bid_amount
        )::jsonb
    FROM listings l
    WHERE l.id = NEW.listing_id;
    
    -- Notify previous highest bidder if outbid
    INSERT INTO notifications (user_id, type, title, message, data)
    SELECT DISTINCT
        b.user_id,
        'BID_OUTBID',
        'You have been outbid',
        'Someone placed a higher bid on ' || l.title,
        json_build_object(
            'listing_id', NEW.listing_id,
            'new_bid_amount', NEW.bid_amount
        )::jsonb
    FROM bids b
    JOIN listings l ON l.id = b.listing_id
    WHERE b.listing_id = NEW.listing_id 
    AND b.user_id != NEW.user_id
    AND b.bid_amount < NEW.bid_amount;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for bid notifications
CREATE TRIGGER bid_notification_trigger
    AFTER INSERT ON bids
    FOR EACH ROW
    EXECUTE FUNCTION create_bid_notification();

-- Function to create message notifications
CREATE OR REPLACE FUNCTION create_message_notification()
RETURNS TRIGGER AS $$
BEGIN
    -- Notify receiver of new message
    INSERT INTO notifications (user_id, type, title, message, data)
    SELECT 
        NEW.receiver_id,
        'NEW_MESSAGE',
        'New message received',
        'You have a new message from ' || COALESCE(a.first_name, 'Someone'),
        json_build_object(
            'message_id', NEW.message_id,
            'sender_id', NEW.sender_id,
            'listing_id', NEW.listing_id
        )::jsonb
    FROM accounts a
    WHERE a.account_id = NEW.sender_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for message notifications
CREATE TRIGGER message_notification_trigger
    AFTER INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION create_message_notification();

-- Insert sample test data
INSERT INTO notifications (user_id, type, title, message, data, is_read) VALUES
(1, 'BID_PLACED', 'New bid on your item', 'Someone placed a bid of ₱1500 on your iPhone 12', '{"listing_id": 1, "bid_amount": 1500}', false),
(1, 'AUCTION_ENDING', 'Auction ending soon', 'Your auction for MacBook Pro ends in 1 hour', '{"listing_id": 2}', false),
(1, 'NEW_MESSAGE', 'New message received', 'You have a new message from John', '{"message_id": 1, "sender_id": 2}', true);

-- Insert default preferences for existing users
INSERT INTO notification_preferences (user_id)
SELECT account_id FROM accounts
ON CONFLICT (user_id) DO NOTHING;