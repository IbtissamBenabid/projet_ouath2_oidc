import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

const API_URL = "http://localhost:8085";

function App({ keycloak }) {
    const [products, setProducts] = useState([]);
    const [orders, setOrders] = useState([]);
    const [error, setError] = useState(null);
    const [showProductForm, setShowProductForm] = useState(false);
    const [editingProduct, setEditingProduct] = useState(null);
    const [productForm, setProductForm] = useState({ name: '', description: '', price: '', quantity: '' });

    const isAdmin = keycloak.tokenParsed?.realm_access?.roles?.includes('ADMIN');

    const authHeaders = { Authorization: `Bearer ${keycloak.token}` };

    useEffect(() => {
        if (keycloak.authenticated) {
            fetchProducts();
            fetchOrders();
        }
    }, [keycloak.authenticated]);

    const handleError = (err, action) => {
        console.error(`Error ${action}`, err);
        if (err.response?.status === 401) {
            setError("Unauthorized. Please log in again.");
            keycloak.logout();
        } else if (err.response?.status === 403) {
            setError(`Forbidden. You do not have permission to ${action}.`);
        } else {
            setError(`Failed to ${action}. ${err.response?.data?.message || err.message}`);
        }
    };

    // ==================== PRODUCTS CRUD ====================
    const fetchProducts = async () => {
        try {
            const res = await axios.get(`${API_URL}/products`, { headers: authHeaders });
            setProducts(res.data);
            setError(null);
        } catch (err) {
            handleError(err, 'fetch products');
        }
    };

    const createProduct = async (e) => {
        e.preventDefault();
        try {
            await axios.post(`${API_URL}/products`, {
                name: productForm.name,
                description: productForm.description,
                price: parseFloat(productForm.price),
                quantity: parseInt(productForm.quantity)
            }, { headers: authHeaders });
            setShowProductForm(false);
            resetProductForm();
            fetchProducts();
            setError(null);
        } catch (err) {
            handleError(err, 'create product');
        }
    };

    const updateProduct = async (e) => {
        e.preventDefault();
        try {
            await axios.put(`${API_URL}/products/${editingProduct.id}`, {
                name: productForm.name,
                description: productForm.description,
                price: parseFloat(productForm.price),
                quantity: parseInt(productForm.quantity)
            }, { headers: authHeaders });
            setEditingProduct(null);
            resetProductForm();
            fetchProducts();
            setError(null);
        } catch (err) {
            handleError(err, 'update product');
        }
    };

    const deleteProduct = async (id) => {
        if (!window.confirm('Are you sure you want to delete this product?')) return;
        try {
            await axios.delete(`${API_URL}/products/${id}`, { headers: authHeaders });
            fetchProducts();
            setError(null);
        } catch (err) {
            handleError(err, 'delete product');
        }
    };

    const startEditProduct = (product) => {
        setEditingProduct(product);
        setProductForm({
            name: product.name,
            description: product.description || '',
            price: product.price.toString(),
            quantity: product.quantity.toString()
        });
        setShowProductForm(false);
    };

    const resetProductForm = () => {
        setProductForm({ name: '', description: '', price: '', quantity: '' });
    };

    const cancelEdit = () => {
        setEditingProduct(null);
        setShowProductForm(false);
        resetProductForm();
    };

    // ==================== ORDERS ====================
    const fetchOrders = async () => {
        try {
            const endpoint = isAdmin ? `${API_URL}/orders/all` : `${API_URL}/orders`;
            const res = await axios.get(endpoint, { headers: authHeaders });
            setOrders(res.data);
            setError(null);
        } catch (err) {
            handleError(err, 'fetch orders');
        }
    };

    const placeOrder = async (productId, price) => {
        const quantity = prompt("Enter quantity:", "1");
        if (!quantity || isNaN(quantity) || parseInt(quantity) <= 0) return;

        try {
            await axios.post(`${API_URL}/orders`, {
                productItems: [{
                    productId: productId,
                    quantity: parseInt(quantity),
                    price: price
                }]
            }, { headers: authHeaders });
            alert("Order placed successfully!");
            fetchOrders();
            fetchProducts(); // Refresh stock
            setError(null);
        } catch (err) {
            handleError(err, 'place order');
        }
    };

    if (!keycloak.authenticated) return <div className="container">Redirecting to login...</div>;

    return (
        <div className="container">
            <header>
                <h1>🛒 E-Commerce Microservices</h1>
                <div className="user-info">
                    <span>
                        {keycloak.tokenParsed?.preferred_username}
                        <small className="role-badge">{isAdmin ? 'ADMIN' : 'CLIENT'}</small>
                    </span>
                    <button onClick={() => keycloak.logout()} className="logout-btn">Logout</button>
                </div>
            </header>

            {error && <div className="error-message">⚠️ {error}</div>}

            <main>
                {/* ==================== PRODUCTS SECTION ==================== */}
                <section>
                    <div className="section-header">
                        <h2>📦 Products Catalog</h2>
                        {isAdmin && !editingProduct && (
                            <button 
                                className="btn-add" 
                                onClick={() => { setShowProductForm(!showProductForm); resetProductForm(); }}
                            >
                                {showProductForm ? '✕ Cancel' : '+ Add Product'}
                            </button>
                        )}
                    </div>

                    {/* Product Form (Create/Edit) */}
                    {isAdmin && (showProductForm || editingProduct) && (
                        <form className="crud-form" onSubmit={editingProduct ? updateProduct : createProduct}>
                            <h3>{editingProduct ? '✏️ Edit Product' : '➕ New Product'}</h3>
                            <div className="form-grid">
                                <input
                                    type="text"
                                    placeholder="Product Name *"
                                    value={productForm.name}
                                    onChange={(e) => setProductForm({...productForm, name: e.target.value})}
                                    required
                                />
                                <input
                                    type="text"
                                    placeholder="Description"
                                    value={productForm.description}
                                    onChange={(e) => setProductForm({...productForm, description: e.target.value})}
                                />
                                <input
                                    type="number"
                                    placeholder="Price *"
                                    step="0.01"
                                    min="0"
                                    value={productForm.price}
                                    onChange={(e) => setProductForm({...productForm, price: e.target.value})}
                                    required
                                />
                                <input
                                    type="number"
                                    placeholder="Stock Quantity *"
                                    min="0"
                                    value={productForm.quantity}
                                    onChange={(e) => setProductForm({...productForm, quantity: e.target.value})}
                                    required
                                />
                            </div>
                            <div className="form-actions">
                                <button type="submit" className="btn-save">
                                    {editingProduct ? '💾 Update' : '➕ Create'}
                                </button>
                                <button type="button" className="btn-cancel" onClick={cancelEdit}>Cancel</button>
                            </div>
                        </form>
                    )}

                    <div className="products-grid">
                        {products.length === 0 ? (
                            <p className="empty-state">No products available.</p>
                        ) : (
                            products.map(p => (
                                <div key={p.id} className="product-card">
                                    <div className="product-info">
                                        <div className="product-name">{p.name}</div>
                                        <div className="product-desc">{p.description}</div>
                                    </div>
                                    <div className="product-details">
                                        <div className="product-price">${p.price?.toFixed(2)}</div>
                                        <div className={`product-stock ${p.quantity <= 5 ? 'low-stock' : ''}`}>
                                            Stock: {p.quantity} {p.quantity <= 5 && '⚠️'}
                                        </div>
                                        <div className="product-actions">
                                            <button 
                                                className="btn-order" 
                                                onClick={() => placeOrder(p.id, p.price)}
                                                disabled={p.quantity <= 0}
                                            >
                                                🛒 Order
                                            </button>
                                            {isAdmin && (
                                                <>
                                                    <button 
                                                        className="btn-edit" 
                                                        onClick={() => startEditProduct(p)}
                                                    >
                                                        ✏️
                                                    </button>
                                                    <button 
                                                        className="btn-delete" 
                                                        onClick={() => deleteProduct(p.id)}
                                                    >
                                                        🗑️
                                                    </button>
                                                </>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </section>

                {/* ==================== ORDERS SECTION ==================== */}
                <section>
                    <div className="section-header">
                        <h2>📋 {isAdmin ? 'All Orders' : 'My Orders'}</h2>
                        <button className="btn-refresh" onClick={fetchOrders}>🔄 Refresh</button>
                    </div>
                    <table className="orders-table">
                        <thead>
                            <tr>
                                <th>Order ID</th>
                                {isAdmin && <th>User</th>}
                                <th>Date</th>
                                <th>Status</th>
                                <th>Amount</th>
                                <th>Items</th>
                            </tr>
                        </thead>
                        <tbody>
                            {orders.length === 0 ? (
                                <tr><td colSpan={isAdmin ? 6 : 5} className="empty-state">No orders found.</td></tr>
                            ) : (
                                orders.map(o => (
                                    <tr key={o.id}>
                                        <td><strong>#{o.id}</strong></td>
                                        {isAdmin && <td>{o.userId}</td>}
                                        <td>{o.date}</td>
                                        <td>
                                            <span className={`status-badge status-${o.status}`}>
                                                {o.status}
                                            </span>
                                        </td>
                                        <td className="amount">${o.amount?.toFixed(2)}</td>
                                        <td>{o.productItems?.length || 0} item(s)</td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </section>
            </main>
        </div>
    );
}

export default App;
