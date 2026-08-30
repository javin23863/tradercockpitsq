


class CMaxDistanceFromMarket : public CTradingOption {
   private:
      
   public:
      CMaxDistanceFromMarket() {
      }

      //+----------------------------------------------+

      virtual bool onBarUpdate() {
         return true;
      }
};

// create variable for class instance (required)
CMaxDistanceFromMarket* objMaxDistanceFromMarket;