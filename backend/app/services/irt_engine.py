import math

class IRTEngine:
    """
    Simplified Item Response Theory (2PL) Engine for Placement Testing.
    """
    
    @staticmethod
    def probability(theta: float, a: float, b: float) -> float:
        """
        Calculates the probability of a correct response.
        P(theta) = 1 / (1 + exp(-a(theta - b)))
        theta: User's ability
        a: Item discrimination
        b: Item difficulty
        """
        try:
            return 1.0 / (1.0 + math.exp(-a * (theta - b)))
        except OverflowError:
            return 0.0 if (theta - b) < 0 else 1.0
            
    @staticmethod
    def estimate_theta(responses: list[dict], current_theta: float = 0.0) -> tuple[float, float]:
        """
        Estimates new theta and Standard Error using Newton-Raphson method (EAP approximation).
        responses: list of dicts: {"is_correct": bool, "a": float, "b": float}
        """
        if not responses:
            return 0.0, 9.0
            
        theta = current_theta
        # Simple gradient descent for max likelihood (simplified for performance)
        learning_rate = 0.5
        info = 0.0
        
        for _ in range(5): # 5 iterations
            gradient = 0.0
            info = 0.0
            for r in responses:
                a = r.get("a", 1.0)
                b = r.get("b", 0.0)
                u = 1.0 if r.get("is_correct") else 0.0
                
                p = IRTEngine.probability(theta, a, b)
                q = 1.0 - p
                
                gradient += a * (u - p)
                info += (a ** 2) * p * q
                
            if info > 0:
                theta_change = gradient / info
                theta += theta_change * learning_rate
                
        se = 1.0 / math.sqrt(info) if info > 0 else 9.0
        return max(min(theta, 3.0), -3.0), se # Clamp theta between -3 (A1) and +3 (C2)
        
    @staticmethod
    def map_theta_to_cefr(theta: float) -> str:
        if theta < -1.5: return "A1"
        if theta < -0.5: return "A2"
        if theta < 0.5: return "B1"
        if theta < 1.5: return "B2"
        if theta < 2.5: return "C1"
        return "C2"
